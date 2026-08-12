package com.cartethyia.easyorange.order.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.lock.DistributedLockPort;
import com.cartethyia.easyorange.framework.lock.LockAcquisitionException;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.aggregate.OrderCreateSpec;
import com.cartethyia.easyorange.order.domain.constant.OrderConstant;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.exception.OrderCreationException;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.exception.PaymentGatewayAdapterException;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.port.ProductOrderPort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * 订单命令处理器 — CQRS Write 侧唯一应用服务，收口全部订单命令（创建/支付/取消/发货/确认收货/退款）。
 * <p>
 * 下单链路：分布式锁排队串行 → 准备商品数据 → 创建订单 → 同步扣减库存 → 创建支付，
 * 全部步骤运行在同一 {@code @Transactional} 事务内，任一步失败由数据库整体回滚兜底
 * （订单 / 库存 / 支付 / Outbox 事件原子提交）。
 * <p>
 * 一致性语义：本地单事务保证原子性；并发下单由 {@link DistributedLockPort} 按 productId 排队串行，
 * 库存扣减由乐观锁版本检查最终兜底防超卖；事件副作用经 Outbox 与应用事务同原子持久化。
 * 为何不使用 Saga 见 ADR-0007。
 * 异常不做二次包装，直接抛给 {@code GlobalExceptionHandler} 按错误码映射。
 * 状态转换命令经 {@link Order} 聚合根守卫执行。
 */
@Service
@RequiredArgsConstructor
public class OrderCommandHandler {

    private static final String ORDER_LOCK_PREFIX = "eo:order:lock:product:";
    private static final long LOCK_TRY_TIMEOUT_SECONDS = 10;
    private static final String LOCK_BUSY_MESSAGE = "资产下单繁忙，请稍后重试";

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OrderCachePort<?> orderCachePort;
    private final DistributedLockPort lockPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final OrderPreparation preparationService;
    private final ProductOrderPort productOrderPort;
    private final IdGenerator idGenerator;

    // ==================== 订单创建 ====================

    /**
     * 执行订单创建 — 分布式锁在事务外获取、提交后释放，创建流程在事务内执行。
     * <p>
     * 锁基础设施的 {@link LockAcquisitionException} 在用例边界映射为 {@link OrderCreationException}，
     * 保留订单域的错误码（B0002→400）与提示文案。
     */
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResult handle(String userId, CreateOrderCommand command) {
        try {
            return lockPort.executeWithLocks(
                    buildLockKeys(command), LOCK_TRY_TIMEOUT_SECONDS, () -> createOrderFlow(userId, command));
        } catch (LockAcquisitionException e) {
            throw new OrderCreationException(LOCK_BUSY_MESSAGE);
        }
    }

    /**
     * 构建锁键列表 — 按 productId 排序避免死锁。
     */
    private List<String> buildLockKeys(CreateOrderCommand command) {
        return command.items().stream()
                .map(CreateOrderCommand.CreateOrderItem::productId)
                .distinct()
                .sorted()
                .map(id -> ORDER_LOCK_PREFIX + id)
                .toList();
    }

    /**
     * 执行下单流程 — 全部步骤在同一事务内，失败由回滚兜底。
     */
    private CreateOrderResult createOrderFlow(String buyerId, CreateOrderCommand command) {
        // 准备订单项数据（含资产存在/在线/库存/同资产方校验）
        OrderPreparation.PreparationResult preparation = preparationService.prepareOrderItems(command.items());

        // 创建订单聚合根（通过 spec record 收敛 7 个参数）
        Transition<Order, OrderCreatedEvent> result = Order.createOrder(new OrderCreateSpec(
                OrderId.of(idGenerator.generateId()),
                UserId.of(buyerId),
                preparation.sellerId(),
                preparation.orderItems(),
                Address.of(resolveAddress(command)),
                Phone.of(command.phone()),
                command.remark()));

        // 保存并发布事件
        orderRepository.save(result.aggregate());
        domainEventPublisher.publish(result.event());

        // 同步扣减库存（同一事务，失败时随事务整体回滚）
        for (var item : command.items()) {
            productOrderPort.decreaseStock(item.productId(), item.quantity());
        }

        // 创建支付（同一事务，失败时随事务整体回滚）
        createPayment(result.event(), command);
        orderCachePort.evictSellerOrders(result.aggregate().sellerId().value());

        return new CreateOrderResult(
                result.aggregate().id().value(), result.aggregate().orderNo().value());
    }

    /**
     * 解析地址：如果未指定则返回默认值。
     */
    private static String resolveAddress(CreateOrderCommand command) {
        return StringUtils.hasText(command.address()) ? command.address() : OrderConstant.DEFAULT_ADDRESS;
    }

    /**
     * 创建支付。
     *
     * @param orderEvent 订单创建事件
     * @param command    创建订单命令
     * @throws PaymentGatewayAdapterException 如果支付创建失败
     */
    private void createPayment(OrderCreatedEvent orderEvent, CreateOrderCommand command) {
        try {
            paymentGatewayPort.createPayment(new PaymentGatewayPort.CreatePaymentRequest(
                    orderEvent.orderId(),
                    orderEvent.totalAmount(),
                    StringUtils.hasText(command.paymentMethod())
                            ? command.paymentMethod()
                            : OrderConstant.DEFAULT_PAYMENT_METHOD,
                    OrderConstant.PAYMENT_BIZ_TYPE,
                    OrderConstant.PAYMENT_DESC,
                    orderEvent.buyerId()));
        } catch (Exception e) {
            throw new PaymentGatewayAdapterException(
                    "支付创建失败 orderId=" + orderEvent.orderId() + ": " + e.getMessage(), e);
        }
    }

    // ==================== 状态转换 ====================

    @Transactional(rollbackFor = Exception.class)
    public void handle(String userId, PayOrderCommand command) {
        var aggregate = validateBuyer(userId, command.orderId());
        var result = aggregate.pay(LocalDateTime.now());
        persistAndPublish(aggregate, result);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(String userId, CancelOrderCommand command) {
        var aggregate = validateBuyer(userId, command.orderId());
        var result = aggregate.cancel(command.reason(), LocalDateTime.now());
        persistAndPublish(aggregate, result);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(String userId, ShipOrderCommand command) {
        var aggregate = validateSeller(userId, command.orderId());
        var result = aggregate.ship(LocalDateTime.now());
        persistAndPublish(aggregate, result);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(String userId, ConfirmReceiptCommand command) {
        var aggregate = validateBuyer(userId, command.orderId());
        var result = aggregate.confirmReceipt(LocalDateTime.now());
        persistAndPublish(aggregate, result);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(String userId, RefundOrderCommand command) {
        var aggregate = validateBuyer(userId, command.orderId());
        var result = aggregate.refund(command.reason(), LocalDateTime.now());
        persistAndPublish(aggregate, result);
    }

    private void persistAndPublish(Order oldAggregate, Transition<Order, ?> result) {
        orderRepository.update(result.aggregate());
        domainEventPublisher.publish(result.event());
        evictCacheAfterCommit(oldAggregate);
    }

    private void evictCacheAfterCommit(Order oldAggregate) {
        var buyerId = oldAggregate.buyerId().value();
        var sellerId = oldAggregate.sellerId().value();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    orderCachePort.evictOrderCache(buyerId, sellerId);
                }
            });
        } else {
            orderCachePort.evictOrderCache(buyerId, sellerId);
        }
    }

    private Order validateBuyer(String userId, String orderId) {
        return validateOwner(userId, orderId, Order::buyerId);
    }

    private Order validateSeller(String userId, String orderId) {
        return validateOwner(userId, orderId, Order::sellerId);
    }

    private Order validateOwner(String userId, String orderId, Function<Order, UserId> ownerExtractor) {
        var aggregate = findOrder(orderId);
        BizRequire.requireTrue(
                Objects.equals(ownerExtractor.apply(aggregate).value(), userId), OrderResultCode.ORDER_NOT_OWNER);
        return aggregate;
    }

    private Order findOrder(String orderId) {
        return orderRepository
                .findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND));
    }
}
