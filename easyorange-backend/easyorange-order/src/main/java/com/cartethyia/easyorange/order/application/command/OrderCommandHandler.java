package com.cartethyia.easyorange.order.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
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
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import com.cartethyia.easyorange.product.enums.ProductStatus;
import com.cartethyia.easyorange.product.application.handler.ProductQueryHandler;
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
    private final ProductQueryHandler productQueryHandler;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResult handle(CreateOrderCommand command) {
        Long buyerId = SecurityContextUtil.getCurrentUserIdOrThrow();

        ProductVO productVO = productQueryHandler.getProductById(command.getProductId());
        BizRequire.notNull(productVO, "商品不存在");
        BizRequire.isTrue(ProductStatus.ONLINE.getCode().equals(productVO.getStatus()), "商品已下架");
        BizRequire.isFalse(productVO.getSellerId().equals(buyerId), "不能购买自己的商品");
        BizRequire.isTrue(productVO.getStock() != null && productVO.getStock() > 0, "商品库存不足");

        OrderCreatedEvent event = OrderAggregate.createOrder(
                buyerId,
                productVO.getSellerId(),
                command.getProductId(),
                productVO.getPrice(),
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

        domainEventPublisher.publish(event);
        log.info("订单已完成 orderId={}", command.getOrderId());
    }

    private Order validateBuyerOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.isTrue(order.getBuyerId().equals(userId), OrderResultCode.ORDER_NOT_OWNER);
        return order;
    }

    private Order validateSellerOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.isTrue(order.getSellerId().equals(userId), OrderResultCode.ORDER_NOT_OWNER);
        return order;
    }
}
