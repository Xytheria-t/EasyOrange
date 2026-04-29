package com.cartethyia.easyorange.order.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.entity.Order;
import com.cartethyia.easyorange.order.enums.OrderResultCode;
import com.cartethyia.easyorange.order.enums.OrderStatus;
import com.cartethyia.easyorange.product.application.port.outbound.ProductSnapshotPort;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandHandler {

    private static final int PAYMENT_STATUS_PAID = 1;

    private final OrderRepository orderRepository;
    private final ProductSnapshotPort productSnapshotPort;
    private final DomainEventPublisher domainEventPublisher;
    private final com.cartethyia.easyorange.order.application.cache.OrderCacheService orderCacheService;

    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResult handle(CreateOrderCommand command) {
        Long buyerId = SecurityContextUtil.getCurrentUserIdOrThrow();

        ProductSnapshotPort.ProductOrderSnapshot snapshot = productSnapshotPort
                .getOrderableSnapshot(new ProductId(command.getProductId()))
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        BizRequire.requireTrue(snapshot.status().isOnline(), "商品已下架");
        BizRequire.ne(snapshot.sellerId().value(), buyerId, "不能购买自己的商品");
        BizRequire.requireTrue(snapshot.stock().isAvailable(), "商品库存不足");

        OrderCreatedEvent event = OrderAggregate.createOrder(
                buyerId,
                snapshot.sellerId().value(),
                command.getProductId(),
                snapshot.price().value(),
                command.getAddress(),
                command.getPhone(),
                command.getRemark()
        );

        OrderAggregate aggregate = OrderAggregate.from(
                event.getOrderId(),
                "ORD" + event.getOrderId(),
                event.getBuyerId(),
                event.getSellerId(),
                event.getProductId(),
                event.getAmount(),
                OrderStatus.PENDING_PAYMENT.getCode(),
                0,
                command.getAddress(),
                command.getPhone(),
                command.getRemark()
        );

        Order order = aggregate.toEntity();
        orderRepository.save(order);

        orderCacheService.deleteSellerOrderCache(snapshot.sellerId().value());

        domainEventPublisher.publish(event);
        log.info("订单创建成功 orderId={} orderNo={}", order.getId(), order.getOrderNo());

        return new CreateOrderResult(order.getId(), order.getOrderNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(PayOrderCommand command) {
        Order order = validateBuyerOrder(command.getOrderId());

        OrderAggregate aggregate = OrderAggregate.fromEntity(order);
        OrderPaidEvent event = aggregate.pay();

        OrderAggregate updatedAggregate = aggregate.withPaymentStatus(PAYMENT_STATUS_PAID);
        orderRepository.update(updatedAggregate.toEntity());

        orderCacheService.deleteOrderCache(order.getBuyerId(), order.getSellerId());

        domainEventPublisher.publish(event);
        log.info("订单已支付 orderId={}", command.getOrderId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(CancelOrderCommand command) {
        Order order = validateBuyerOrder(command.getOrderId());

        OrderAggregate aggregate = OrderAggregate.fromEntity(order);
        OrderCancelledEvent event = aggregate.cancel(command.getReason());

        OrderAggregate updatedAggregate = aggregate.withStatus(OrderStatus.CANCELLED.getCode());
        orderRepository.update(updatedAggregate.toEntity());

        orderCacheService.deleteOrderCache(order.getBuyerId(), order.getSellerId());

        domainEventPublisher.publish(event);
        log.info("订单已取消 orderId={} reason={}", command.getOrderId(), command.getReason());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ShipOrderCommand command) {
        Order order = validateSellerOrder(command.getOrderId());

        OrderAggregate aggregate = OrderAggregate.fromEntity(order);
        OrderShippedEvent event = aggregate.ship();

        OrderAggregate updatedAggregate = aggregate.withStatus(OrderStatus.SHIPPED.getCode());
        orderRepository.update(updatedAggregate.toEntity());

        orderCacheService.deleteOrderCache(order.getBuyerId(), order.getSellerId());

        domainEventPublisher.publish(event);
        log.info("订单已发货 orderId={}", command.getOrderId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ConfirmReceiptCommand command) {
        Order order = validateBuyerOrder(command.getOrderId());

        OrderAggregate aggregate = OrderAggregate.fromEntity(order);
        OrderCompletedEvent event = aggregate.confirmReceipt();

        OrderAggregate updatedAggregate = aggregate.withStatus(OrderStatus.COMPLETED.getCode());
        orderRepository.update(updatedAggregate.toEntity());

        orderCacheService.deleteOrderCache(order.getBuyerId(), order.getSellerId());

        domainEventPublisher.publish(event);
        log.info("订单已完成 orderId={}", command.getOrderId());
    }

    private Order validateBuyerOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.eq(order.getBuyerId(), userId, OrderResultCode.ORDER_NOT_OWNER);
        return order;
    }

    private Order validateSellerOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.eq(order.getSellerId(), userId, OrderResultCode.ORDER_NOT_OWNER);
        return order;
    }
}
