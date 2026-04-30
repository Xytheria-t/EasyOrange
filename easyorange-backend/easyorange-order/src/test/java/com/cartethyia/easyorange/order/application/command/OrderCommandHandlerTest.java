package com.cartethyia.easyorange.order.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.outbound.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.enums.OrderStatus;
import com.cartethyia.easyorange.order.infrastructure.cache.OrderCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCommandHandler 单元测试")
class OrderCommandHandlerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private PaymentGatewayPort paymentGatewayPort;

    @Mock
    private OrderCacheService orderCacheService;

    private OrderCommandHandler handler;

    private static final Long ORDER_ID = 1L;
    private static final Long BUYER_ID = 10L;
    private static final Long SELLER_ID = 20L;
    private static final Long PRODUCT_ID = 100L;

    @BeforeEach
    void setUp() {
        handler = new OrderCommandHandler(orderRepository, domainEventPublisher, null, paymentGatewayPort, orderCacheService);

        Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                BUYER_ID, null, authorities
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private OrderAggregate pendingPaymentOrder() {
        return OrderAggregate.fromRaw(
                ORDER_ID, "ORD1", BUYER_ID, SELLER_ID, PRODUCT_ID,
                new BigDecimal("99.99"), OrderStatus.PENDING_PAYMENT.getCode(), 0,
                "北京市朝阳区", "13800138000", "备注", null, null
        );
    }

    private OrderAggregate paidOrder() {
        return OrderAggregate.fromRaw(
                ORDER_ID, "ORD1", BUYER_ID, SELLER_ID, PRODUCT_ID,
                new BigDecimal("99.99"), OrderStatus.PAID.getCode(), 1,
                "北京市朝阳区", "13800138000", "备注", null, null
        );
    }

    private OrderAggregate shippedOrder() {
        return OrderAggregate.fromRaw(
                ORDER_ID, "ORD1", BUYER_ID, SELLER_ID, PRODUCT_ID,
                new BigDecimal("99.99"), OrderStatus.SHIPPED.getCode(), 1,
                "北京市朝阳区", "13800138000", "备注", null, null
        );
    }

    @Nested
    @DisplayName("pay")
    class PayTests {

        @Test
        @DisplayName("待付款订单可以支付")
        void pay_pendingPayment_success() {
            OrderAggregate aggregate = pendingPaymentOrder();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            PayOrderCommand command = PayOrderCommand.builder().orderId(ORDER_ID).build();

            handler.handle(command);

            verify(orderRepository).update(any(OrderAggregate.class));
            verify(domainEventPublisher).publish(any(OrderPaidEvent.class));
            verify(orderCacheService).deleteOrderCache(BUYER_ID, SELLER_ID);
        }

        @Test
        @DisplayName("订单不存在时支付抛异常")
        void pay_orderNotFound_throws() {
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.empty());

            PayOrderCommand command = PayOrderCommand.builder().orderId(ORDER_ID).build();

            assertThatThrownBy(() -> handler.handle(command))
                    .isInstanceOf(OrderDomainException.class);
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTests {

        @Test
        @DisplayName("待付款订单可以取消")
        void cancel_pendingPayment_success() {
            OrderAggregate aggregate = pendingPaymentOrder();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            CancelOrderCommand command = CancelOrderCommand.builder()
                    .orderId(ORDER_ID).reason("不想要了").build();

            handler.handle(command);

            verify(orderRepository).update(any(OrderAggregate.class));
            verify(domainEventPublisher).publish(any(OrderCancelledEvent.class));
            verify(orderCacheService).deleteOrderCache(BUYER_ID, SELLER_ID);
        }
    }

    @Nested
    @DisplayName("ship")
    class ShipTests {

        @Test
        @DisplayName("已付款订单可以发货")
        void ship_paid_success() {
            Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    SELLER_ID, null, authorities
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            OrderAggregate aggregate = paidOrder();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            ShipOrderCommand command = ShipOrderCommand.builder().orderId(ORDER_ID).build();

            handler.handle(command);

            verify(orderRepository).update(any(OrderAggregate.class));
            verify(domainEventPublisher).publish(any(OrderShippedEvent.class));
            verify(orderCacheService).deleteOrderCache(BUYER_ID, SELLER_ID);
        }
    }

    @Nested
    @DisplayName("confirmReceipt")
    class ConfirmReceiptTests {

        @Test
        @DisplayName("已发货订单可以确认收货")
        void confirmReceipt_shipped_success() {
            OrderAggregate aggregate = shippedOrder();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            ConfirmReceiptCommand command = ConfirmReceiptCommand.builder().orderId(ORDER_ID).build();

            handler.handle(command);

            verify(orderRepository).update(any(OrderAggregate.class));
            verify(domainEventPublisher).publish(any(OrderCompletedEvent.class));
            verify(orderCacheService).deleteOrderCache(BUYER_ID, SELLER_ID);
        }
    }

    @Nested
    @DisplayName("refund")
    class RefundTests {

        @Test
        @DisplayName("已付款订单可以退款")
        void refund_paid_success() {
            OrderAggregate aggregate = paidOrder();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            RefundOrderCommand command = RefundOrderCommand.builder()
                    .orderId(ORDER_ID).reason("商品有问题").build();

            handler.handle(command);

            verify(paymentGatewayPort).refundPayment(ORDER_ID, "商品有问题");
            verify(orderRepository).update(any(OrderAggregate.class));
            verify(domainEventPublisher).publish(any(OrderRefundedEvent.class));
            verify(orderCacheService).deleteOrderCache(BUYER_ID, SELLER_ID);
        }

        @Test
        @DisplayName("已发货订单可以退款")
        void refund_shipped_success() {
            OrderAggregate aggregate = shippedOrder();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            RefundOrderCommand command = RefundOrderCommand.builder()
                    .orderId(ORDER_ID).reason("快递损坏").build();

            handler.handle(command);

            verify(paymentGatewayPort).refundPayment(ORDER_ID, "快递损坏");
            verify(orderRepository).update(any(OrderAggregate.class));
            verify(domainEventPublisher).publish(any(OrderRefundedEvent.class));
        }

        @Test
        @DisplayName("待付款订单不能退款")
        void refund_pendingPayment_throws() {
            OrderAggregate aggregate = pendingPaymentOrder();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            RefundOrderCommand command = RefundOrderCommand.builder()
                    .orderId(ORDER_ID).reason("测试").build();

            assertThatThrownBy(() -> handler.handle(command))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("validateBuyerOrder")
    class ValidateBuyerOrderTests {

        @Test
        @DisplayName("订单不存在时抛 OrderDomainException")
        void validateBuyerOrder_notFound_throws() {
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.empty());

            PayOrderCommand command = PayOrderCommand.builder().orderId(ORDER_ID).build();

            assertThatThrownBy(() -> handler.handle(command))
                    .isInstanceOf(OrderDomainException.class);
        }
    }
}
