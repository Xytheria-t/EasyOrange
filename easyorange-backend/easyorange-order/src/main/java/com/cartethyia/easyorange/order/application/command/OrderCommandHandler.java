package com.cartethyia.easyorange.order.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.outbound.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.enums.OrderResultCode;
import com.cartethyia.easyorange.order.domain.saga.CreateOrderSaga;
import com.cartethyia.easyorange.order.infrastructure.cache.OrderCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandHandler {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final CreateOrderSaga createOrderSaga;
    private final PaymentGatewayPort paymentGatewayPort;
    private final OrderCacheService orderCacheService;

    public CreateOrderResult handle(CreateOrderCommand command) {
        return createOrderSaga.execute(command);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(PayOrderCommand command) {
        OrderAggregate aggregate = validateBuyerOrder(command.getOrderId());
        OrderAggregate.OrderPaidResult result = aggregate.pay();
        orderRepository.update(result.aggregate());

        orderCacheService.deleteOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
        domainEventPublisher.publish(result.event());
        log.info("订单已支付 orderId={}", command.getOrderId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(CancelOrderCommand command) {
        OrderAggregate aggregate = validateBuyerOrder(command.getOrderId());
        OrderAggregate.OrderCancelledResult result = aggregate.cancel(command.getReason());
        orderRepository.update(result.aggregate());

        orderCacheService.deleteOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
        domainEventPublisher.publish(result.event());
        log.info("订单已取消 orderId={} reason={}", command.getOrderId(), command.getReason());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ShipOrderCommand command) {
        OrderAggregate aggregate = validateSellerOrder(command.getOrderId());
        OrderAggregate.OrderShippedResult result = aggregate.ship();
        orderRepository.update(result.aggregate());

        orderCacheService.deleteOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
        domainEventPublisher.publish(result.event());
        log.info("订单已发货 orderId={}", command.getOrderId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ConfirmReceiptCommand command) {
        OrderAggregate aggregate = validateBuyerOrder(command.getOrderId());
        OrderAggregate.OrderCompletedResult result = aggregate.confirmReceipt();
        orderRepository.update(result.aggregate());

        orderCacheService.deleteOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
        domainEventPublisher.publish(result.event());
        log.info("订单已完成 orderId={}", command.getOrderId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(RefundOrderCommand command) {
        OrderAggregate aggregate = validateBuyerOrder(command.getOrderId());
        OrderAggregate.OrderRefundedResult result = aggregate.refund(command.getReason());

        paymentGatewayPort.refundPayment(aggregate.id().value(), command.getReason());

        orderRepository.update(result.aggregate());
        orderCacheService.deleteOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
        domainEventPublisher.publish(result.event());
        log.info("订单已退款 orderId={} reason={}", command.getOrderId(), command.getReason());
    }

    private OrderAggregate validateBuyerOrder(Long orderId) {
        OrderAggregate aggregate = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND));
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.eq(aggregate.buyerId().value(), userId, OrderResultCode.ORDER_NOT_OWNER);
        return aggregate;
    }

    private OrderAggregate validateSellerOrder(Long orderId) {
        OrderAggregate aggregate = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND));
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.eq(aggregate.sellerId().value(), userId, OrderResultCode.ORDER_NOT_OWNER);
        return aggregate;
    }
}
