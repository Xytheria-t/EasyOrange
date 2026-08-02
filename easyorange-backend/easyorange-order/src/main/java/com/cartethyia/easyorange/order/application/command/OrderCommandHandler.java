package com.cartethyia.easyorange.order.application.command;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.application.service.OrderCreationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderCommandHandler {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OrderCreationService orderCreationService;
    private final PaymentGatewayPort paymentGatewayPort;
    private final OrderCachePort<?> orderCachePort;

    public CreateOrderResult handle(CreateOrderCommand command) {
        return orderCreationService.execute(command);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(PayOrderCommand command) {
        var aggregate = validateBuyer(command.orderId());
        var result = aggregate.pay();
        persistAndPublish(aggregate, result);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(CancelOrderCommand command) {
        var aggregate = validateBuyer(command.orderId());
        var result = aggregate.cancel(command.reason());
        persistAndPublish(aggregate, result);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ShipOrderCommand command) {
        var aggregate = validateSeller(command.orderId());
        var result = aggregate.ship();
        persistAndPublish(aggregate, result);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ConfirmReceiptCommand command) {
        var aggregate = validateBuyer(command.orderId());
        var result = aggregate.confirmReceipt();
        persistAndPublish(aggregate, result);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(RefundOrderCommand command) {
        var aggregate = validateBuyer(command.orderId());
        var result = aggregate.refund(command.reason());
        paymentGatewayPort.refundPayment(aggregate.id().value(), command.reason());
        persistAndPublish(aggregate, result);
    }

    private void persistAndPublish(Order oldAggregate,
                                   Transition<Order, ?> result) {
        orderRepository.update(result.aggregate());
        orderCachePort.evictOrderCache(oldAggregate.buyerId().value(), oldAggregate.sellerId().value());
        domainEventPublisher.publish(result.event());
    }

    private Order validateBuyer(String orderId) {
        var aggregate = findOrder(orderId);
        var userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.requireTrue(Objects.equals(aggregate.buyerId().value(), userId), OrderResultCode.ORDER_NOT_OWNER);
        return aggregate;
    }

    private Order validateSeller(String orderId) {
        var aggregate = findOrder(orderId);
        var userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.requireTrue(Objects.equals(aggregate.sellerId().value(), userId), OrderResultCode.ORDER_NOT_OWNER);
        return aggregate;
    }

    private Order findOrder(String orderId) {
        return orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND));
    }
}
