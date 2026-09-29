package com.cartethyia.easyorange.order.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.lock.DistributedLockPort;
import com.cartethyia.easyorange.framework.lock.LockAcquisitionException;
import com.cartethyia.easyorange.order.application.service.OrderCacheEvictor;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.aggregate.OrderCreateSpec;
import com.cartethyia.easyorange.order.domain.constant.OrderConstant;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.domain.enums.OrderStatus;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiPredicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * 订单命令处理器 — CQRS Write 侧唯一应用服务，收口订单全部命令；
 * 事件驱动入口 {@link #onPaymentSucceeded} 单列，不混入命令入口。
 * <p>
 * 下单链路：分布式锁排队串行 → 准备商品数据 → 创建订单 → 扣减库存 → 创建支付，全部步骤同一本地事务（边界由
 * {@link TransactionTemplate} 显式控制，锁等待在事务外），任一步失败由数据库回滚兜底，Outbox 事件同事务原子提交。
 * <p>
 * 并发下单由 {@link DistributedLockPort} 按 productId 排队串行，库存扣减另由乐观锁版本检查防超卖兜底；库存流水以订单 ID
 * 为幂等键，保证同一订单只扣一次、恢复数量与扣减对称。不用 Saga 见 ADR-0007。
 * <p>
 * 异常上抛：领域异常直接上抛由 {@code GlobalExceptionHandler} 按错误码映射；锁争用与上游不可用在用例边界
 * 各自映射为订单域错误码，不对已有业务异常二次包装。状态转换一律经 {@link Order} 聚合根守卫。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandHandler {

    private static final String ORDER_LOCK_PREFIX = "eo:order:lock:product:";
    private static final long LOCK_TRY_TIMEOUT_SECONDS = 10;
    private static final String LOCK_BUSY_MESSAGE = "资产下单繁忙，请稍后重试";

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OrderCacheEvictor orderCacheEvictor;
    private final DistributedLockPort lockPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final OrderItemPreparer itemPreparer;
    private final ProductInventoryPort productInventoryPort;
    private final IdGenerator idGenerator;
    private final TransactionTemplate transactionTemplate;

    // ── 订单创建 ──

    /**
     * 分布式锁在事务外获取、提交后释放，锁内只跑事务内的创建流程。
     * <p>
     * 锁争用（{@link LockAcquisitionException}）在此映射为 {@link OrderDomainException}，
     * 保留订单域错误码（B3009→400）与提示文案。
     */
    public CreateOrderResult createOrder(String userId, CreateOrderCommand command) {
        try {
            return lockPort.executeWithLocks(
                    buildLockKeys(command),
                    LOCK_TRY_TIMEOUT_SECONDS,
                    () -> transactionTemplate.execute(_ -> createOrderFlow(userId, command)));
        } catch (LockAcquisitionException e) {
            throw OrderDomainException.of(LOCK_BUSY_MESSAGE);
        }
    }

    /** 锁键按 productId 排序获取，避免多资产并发下单互相等待形成死锁。 */
    private List<String> buildLockKeys(CreateOrderCommand command) {
        return command.items().stream()
                .map(CreateOrderCommand.CreateOrderItem::productId)
                .distinct()
                .sorted()
                .map(id -> ORDER_LOCK_PREFIX + id)
                .toList();
    }

    private CreateOrderResult createOrderFlow(String buyerId, CreateOrderCommand command) {
        // 准备订单项数据（含资产存在/在线/库存/同资产方校验）
        OrderItemPreparer.PreparationResult preparation = itemPreparer.prepareOrderItems(command.items());

        Transition<Order, OrderCreatedEvent> result = Order.createOrder(new OrderCreateSpec(
                OrderId.of(idGenerator.generateId()),
                UserId.of(buyerId),
                preparation.sellerId(),
                preparation.orderItems(),
                Address.of(resolveAddress(command)),
                Phone.of(command.phone()),
                command.remark()));

        orderRepository.save(result.aggregate());
        domainEventPublisher.publish(result.event());

        // 订单 ID 即库存流水的幂等键：事件重投 / 重试不会二次扣减
        for (var item : command.items()) {
            productInventoryPort.decreaseStock(result.event().orderId(), item.productId(), item.quantity());
        }

        createPayment(result.event());
        // 买家/卖家订单列表缓存提交后再失效，避免提交前失效被并发读以旧数据重新填充
        orderCacheEvictor.evictOrderCacheAfterCommit(result.aggregate());

        return new CreateOrderResult(
                result.aggregate().id().value(), result.aggregate().orderNo().value());
    }

    private static String resolveAddress(CreateOrderCommand command) {
        return StringUtils.hasText(command.address()) ? command.address() : OrderConstant.DEFAULT_ADDRESS;
    }

    /**
     * 创建支付 —— 网关调用失败在此统一收敛为上游不可用，避免异常类型外泄到用例编排层。
     *
     * @throws OrderDomainException 支付创建失败（上游不可用，D0502→502）
     */
    private void createPayment(OrderCreatedEvent orderEvent) {
        try {
            paymentGatewayPort.createPayment(new PaymentGatewayPort.CreatePaymentRequest(
                    orderEvent.orderId(),
                    orderEvent.totalAmount(),
                    OrderConstant.DEFAULT_PAYMENT_METHOD,
                    OrderConstant.PAYMENT_BIZ_TYPE,
                    OrderConstant.PAYMENT_DESC,
                    orderEvent.buyerId()));
        } catch (Exception e) {
            throw OrderDomainException.upstream("支付创建失败 orderId=" + orderEvent.orderId() + ": " + e.getMessage(), e);
        }
    }

    // ── 状态转换 ──

    /**
     * 发起支付 — 校验买家身份与可支付状态后委托支付模块走两阶段；订单置 PAID 不在此发生，
     * 而由 {@link #onPaymentSucceeded} 事件桥接驱动，保证订单与支付单状态联动一致。本方法无本地写，不开事务。
     */
    public void payOrder(String userId, PayOrderCommand command) {
        var aggregate = validateParticipant(userId, command.orderId(), Order::isBuyer);
        BizRequire.requireTrue(aggregate.canPay(), OrderResultCode.ORDER_STATUS_ERROR);
        paymentGatewayPort.pay(command.orderId());
    }

    /**
     * 支付成功事件桥接 — 订单置 PAID 的唯一入口。已支付直接跳过（幂等，覆盖重复投递与重复支付单）；
     * 已取消时款已扣但订单不再流转，触发自动退款补偿（库存已在取消时恢复，订单保持取消态）；其余非法状态由
     * {@link Order#pay} 守卫抛 {@code ORDER_STATUS_ERROR}，消费失败进 DLQ。
     * <p>
     * 不整体开事务：退款分支是纯网关动作，网关调用必须在事务外；置 PAID 分支才用 {@link TransactionTemplate}，
     * 并在事务内重读订单，使 {@link Order#pay} 的状态守卫以事务内快照为准。
     */
    public void onPaymentSucceeded(String orderId) {
        var aggregate = findOrder(orderId);
        if (aggregate.status() == OrderStatus.PAID) {
            log.info("支付成功事件跳过（订单已支付）: orderId={}", orderId);
            return;
        }
        if (aggregate.status() == OrderStatus.CANCELLED) {
            // 款已扣但订单已取消：只补偿退款不改状态；纯网关调用留在事务里会挂住 DB 连接，且回滚撤不回已发生的退款
            log.info("支付成功但订单已取消，自动退款: orderId={}", orderId);
            paymentGatewayPort.refundPayment(orderId, OrderConstant.AUTO_REFUND_REASON);
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            var fresh = findOrder(orderId);
            var result = fresh.pay(LocalDateTime.now());
            persistAndPublish(fresh, result);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String userId, CancelOrderCommand command) {
        var aggregate = validateParticipant(userId, command.orderId(), Order::isBuyer);
        var result = aggregate.cancel(command.reason(), LocalDateTime.now());
        persistAndPublish(aggregate, result);
    }

    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(String userId, ShipOrderCommand command) {
        var aggregate = validateParticipant(userId, command.orderId(), Order::isSeller);
        var result = aggregate.ship(LocalDateTime.now());
        persistAndPublish(aggregate, result);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(String userId, ConfirmReceiptCommand command) {
        var aggregate = validateParticipant(userId, command.orderId(), Order::isBuyer);
        var result = aggregate.confirmReceipt(LocalDateTime.now());
        persistAndPublish(aggregate, result);
    }

    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(String userId, RefundOrderCommand command) {
        var aggregate = validateParticipant(userId, command.orderId(), Order::isBuyer);
        var result = aggregate.refund(command.reason(), LocalDateTime.now());
        persistAndPublish(aggregate, result);
    }

    private void persistAndPublish(Order oldAggregate, Transition<Order, ?> result) {
        orderRepository.update(result.aggregate());
        domainEventPublisher.publish(result.event());
        orderCacheEvictor.evictOrderCacheAfterCommit(oldAggregate);
    }

    /** 买家 / 卖家身份校验同走这一道守卫，只差角色谓词；越权一律 ORDER_NOT_OWNER 不泄露他人订单存在性。 */
    private Order validateParticipant(String userId, String orderId, BiPredicate<Order, String> isRole) {
        var aggregate = findOrder(orderId);
        BizRequire.requireTrue(isRole.test(aggregate, userId), OrderResultCode.ORDER_NOT_OWNER);
        return aggregate;
    }

    private Order findOrder(String orderId) {
        return orderRepository.findById(OrderId.of(orderId)).orElseThrow(() -> OrderDomainException.notFound(orderId));
    }
}
