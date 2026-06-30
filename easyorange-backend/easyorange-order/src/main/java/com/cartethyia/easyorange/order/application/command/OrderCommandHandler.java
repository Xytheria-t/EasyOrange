package com.cartethyia.easyorange.order.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.application.saga.CreateOrderSaga;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        OrderAggregate aggregate = validateBuyerOrder(command.getOrderId());
        OrderAggregate.OrderPaidResult result = aggregate.pay();
        orderRepository.update(result.aggregate());

        orderCachePort.evictOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
        domainEventPublisher.publish(result.event());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(CancelOrderCommand command) {
        OrderAggregate aggregate = validateBuyerOrder(command.getOrderId());
        OrderAggregate.OrderCancelledResult result = aggregate.cancel(command.getReason());
        orderRepository.update(result.aggregate());

        orderCachePort.evictOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
        domainEventPublisher.publish(result.event());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ShipOrderCommand command) {
        OrderAggregate aggregate = validateSellerOrder(command.getOrderId());
        OrderAggregate.OrderShippedResult result = aggregate.ship();
        orderRepository.update(result.aggregate());

        orderCachePort.evictOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
        domainEventPublisher.publish(result.event());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ConfirmReceiptCommand command) {
        OrderAggregate aggregate = validateBuyerOrder(command.getOrderId());
        OrderAggregate.OrderCompletedResult result = aggregate.confirmReceipt();
        orderRepository.update(result.aggregate());

        orderCachePort.evictOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
        domainEventPublisher.publish(result.event());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(RefundOrderCommand command) {
        OrderAggregate aggregate = validateBuyerOrder(command.getOrderId());
        BizRequire.requireTrue(aggregate.paymentStatus() == PaymentStatus.PAID, OrderResultCode.ORDER_CANNOT_REFUND);
        OrderAggregate.OrderRefundedResult result = aggregate.refund(command.getReason());

        paymentGatewayPort.refundPayment(aggregate.id().value(), command.getReason());

        orderRepository.update(result.aggregate());
        orderCachePort.evictOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
        domainEventPublisher.publish(result.event());
    }

    private OrderAggregate validateBuyerOrder(String orderId) {
        OrderAggregate aggregate = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND));
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.requireTrue(java.util.Objects.equals(aggregate.buyerId().value(), userId), OrderResultCode.ORDER_NOT_OWNER);
        return aggregate;
    }

    private OrderAggregate validateSellerOrder(String orderId) {
        OrderAggregate aggregate = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND));
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.requireTrue(java.util.Objects.equals(aggregate.sellerId().value(), userId), OrderResultCode.ORDER_NOT_OWNER);
        return aggregate;
    }
}
