package com.cartethyia.easyorange.order.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import com.cartethyia.easyorange.order.application.service.OrderCreationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class OrderCommandHandler {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OrderCreationService orderCreationService;
    private final OrderCachePort<?> orderCachePort;

    public CreateOrderResult handle(CreateOrderCommand command) {
        return orderCreationService.createOrder(command);
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
        persistAndPublish(aggregate, result);
    }

    private void persistAndPublish(Order oldAggregate,
                                   Transition<Order, ?> result) {
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

    private Order validateBuyer(String orderId) {
        return validateOwner(orderId, Order::buyerId);
    }

    private Order validateSeller(String orderId) {
        return validateOwner(orderId, Order::sellerId);
    }

    private Order validateOwner(String orderId, Function<Order, UserId> ownerExtractor) {
        var aggregate = findOrder(orderId);
        var userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.requireTrue(Objects.equals(ownerExtractor.apply(aggregate).value(), userId), OrderResultCode.ORDER_NOT_OWNER);
        return aggregate;
    }

    private Order findOrder(String orderId) {
        return orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND));
    }
}
