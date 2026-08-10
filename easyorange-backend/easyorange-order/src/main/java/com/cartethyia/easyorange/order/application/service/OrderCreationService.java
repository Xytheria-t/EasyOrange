package com.cartethyia.easyorange.order.application.service;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.aggregate.OrderCreateSpec;
import com.cartethyia.easyorange.order.domain.constant.OrderConstant;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.exception.PaymentGatewayAdapterException;
import com.cartethyia.easyorange.order.domain.port.LockPort;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.port.ProductOrderPort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 订单创建服务
 * <p>
 * 下单链路：分布式锁防超卖 → 准备商品数据 → 创建订单 → 同步扣减库存 → 创建支付，
 * 全部步骤运行在同一 {@code @Transactional} 事务内，任一步失败由数据库整体回滚兜底
 * （订单 / 库存 / 支付 / Outbox 事件原子提交）。
 * <p>
 * 一致性语义：本地单事务保证原子性；并发防超卖由 {@link LockPort} 承担；
 * 事件副作用经 Outbox 与应用事务同原子持久化。为何不使用 Saga 见 ADR-0007。
 * 异常不做二次包装，直接抛给 {@code GlobalExceptionHandler} 按错误码映射。
 */
@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private static final String ORDER_LOCK_PREFIX = "eo:order:lock:product:";
    private static final long LOCK_TRY_TIMEOUT_SECONDS = 10;

    private final LockPort lockPort;
    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    private final PaymentGatewayPort paymentGatewayPort;
    private final OrderCachePort<?> orderCachePort;
    private final OrderPreparation preparationService;
    private final ProductOrderPort productOrderPort;
    private final IdGenerator idGenerator;

    /**
     * 执行订单创建
     */
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        return lockPort.executeWithLocks(
                buildLockKeys(command), LOCK_TRY_TIMEOUT_SECONDS, () -> createOrderFlow(command));
    }

    /**
     * 构建锁键列表 — 按 productId 排序避免死锁
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
     * 执行下单流程 — 全部步骤在同一事务内，失败由回滚兜底
     */
    private CreateOrderResult createOrderFlow(CreateOrderCommand command) {
        String buyerId = SecurityContextUtil.getCurrentUserIdOrThrow();

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
        eventPublisher.publish(result.event());

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
                    OrderConstant.PAYMENT_DESC));
        } catch (Exception e) {
            throw new PaymentGatewayAdapterException(
                    "支付创建失败 orderId=" + orderEvent.orderId() + ": " + e.getMessage(), e);
        }
    }

    /**
     * 解析地址：如果未指定则返回默认值。
     */
    private static String resolveAddress(CreateOrderCommand command) {
        return StringUtils.hasText(command.address()) ? command.address() : OrderConstant.DEFAULT_ADDRESS;
    }
}
