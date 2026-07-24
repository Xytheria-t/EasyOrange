package com.cartethyia.easyorange.order.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.application.saga.CreateOrderSaga;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class OrderCommandHandler {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final CreateOrderSaga createOrderSaga;
    private final PaymentGatewayPort paymentGatewayPort;
    private final OrderCachePort<?> orderCachePort;

    public CreateOrderResult handle(CreateOrderCommand command) {
        return createOrderSaga.execute(command);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(PayOrderCommand command) {
        transitionOrder(command.orderId(), OrderRole.BUYER, OrderAggregate::pay);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(CancelOrderCommand command) {
        transitionOrder(command.orderId(), OrderRole.BUYER, agg -> agg.cancel(command.reason()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ShipOrderCommand command) {
        transitionOrder(command.orderId(), OrderRole.SELLER, OrderAggregate::ship);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ConfirmReceiptCommand command) {
        transitionOrder(command.orderId(), OrderRole.BUYER, OrderAggregate::confirmReceipt);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(RefundOrderCommand command) {
        OrderAggregate aggregate = validateOrderOwnership(command.orderId(), OrderRole.BUYER);
        BizRequire.requireTrue(aggregate.canRefund(), OrderResultCode.ORDER_CANNOT_REFUND);

        OrderAggregate.OrderTransition<OrderRefundedEvent> result = aggregate.refund(command.reason());
        paymentGatewayPort.refundPayment(aggregate.id().value(), command.reason());

        orderRepository.update(result.aggregate());
        evictCacheAndPublish(aggregate, result.event());
    }

    /**
     * 状态转换模板方法 — 统一 "校验 → 转换 → 更新 → 失效缓存 → 发布事件" 五步。
     */
    private <E extends com.cartethyia.easyorange.common.event.DomainEvent> void transitionOrder(
            String orderId, OrderRole role, Function<OrderAggregate, OrderAggregate.OrderTransition<E>> action) {
        OrderAggregate aggregate = validateOrderOwnership(orderId, role);
        OrderAggregate.OrderTransition<E> result = action.apply(aggregate);
        orderRepository.update(result.aggregate());
        evictCacheAndPublish(aggregate, result.event());
    }

    private void evictCacheAndPublish(OrderAggregate aggregate,
                                       com.cartethyia.easyorange.common.event.DomainEvent event) {
        orderCachePort.evictOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
        domainEventPublisher.publish(event);
    }

    /**
     * 校验订单归属 — 合并 validateBuyerOrder / validateSellerOrder 重复逻辑。
     */
    private OrderAggregate validateOrderOwnership(String orderId, OrderRole role) {
        OrderAggregate aggregate = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND));
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        String ownerId = role == OrderRole.BUYER ? aggregate.buyerId().value() : aggregate.sellerId().value();
        BizRequire.requireTrue(Objects.equals(ownerId, userId), OrderResultCode.ORDER_NOT_OWNER);
        return aggregate;
    }

    /**
     * 订单归属角色 — 区分校验 buyerId 还是 sellerId。
     */
    private enum OrderRole { BUYER, SELLER }
}
