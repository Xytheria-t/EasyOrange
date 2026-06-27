package com.cartethyia.easyorange.order.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;

import com.cartethyia.easyorange.framework.util.TestSecurityUtil;

import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.application.saga.CreateOrderSaga;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.ProductId;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.cartethyia.easyorange.order.application.command.CreateOrderCommand.CreateOrderItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCommandHandler 单元测试")
class OrderCommandHandlerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private CreateOrderSaga createOrderSaga;

    @Mock
    private PaymentGatewayPort paymentGatewayPort;

    @Mock
    private OrderCachePort orderCachePort;

    @InjectMocks
    private OrderCommandHandler commandHandler;

    private static final Long BUYER_ID = 1L;
    private static final Long SELLER_ID = 2L;
    private static final Long ORDER_ID = 100L;
    private static final Long PRODUCT_ID = 200L;

    @BeforeEach
    void setUp() {
    }

    @Nested
    @DisplayName("handle(CreateOrderCommand)")
    class CreateOrderTests {

        @Test
        @DisplayName("正常创建订单")
        void handle_createOrder_success() {
            CreateOrderCommand command = CreateOrderCommand.builder()
                .items(List.of(new CreateOrderItem(PRODUCT_ID, 1)))
                .address("北京市朝阳区")
                .phone("13800138000")
                .remark("尽快发货")
                .build();

            CreateOrderResult expectedResult = new CreateOrderResult(ORDER_ID, "ORD123");
            when(createOrderSaga.execute(command)).thenReturn(expectedResult);

            CreateOrderResult result = commandHandler.handle(command);

            assertThat(result.orderId()).isEqualTo(ORDER_ID);
            assertThat(result.orderNo()).isEqualTo("ORD123");
            verify(createOrderSaga).execute(command);
        }
    }

    @Nested
    @DisplayName("handle(PayOrderCommand)")
    class PayOrderTests {

        @Test
        @DisplayName("正常支付订单")
        void handle_payOrder_success() {
            PayOrderCommand command = PayOrderCommand.builder()
                .orderId(ORDER_ID)
                .build();

            OrderAggregate aggregate = createPendingPaymentAggregate();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(BUYER_ID);
            try {
                commandHandler.handle(command);

                verify(orderRepository).update(any(OrderAggregate.class));
                verify(domainEventPublisher).publish(any(OrderPaidEvent.class));
                verify(orderCachePort).evictOrderCache(BUYER_ID, SELLER_ID);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("订单不存在时抛出异常")
        void handle_payOrder_orderNotFound() {
            PayOrderCommand command = PayOrderCommand.builder()
                .orderId(ORDER_ID)
                .build();

            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.empty());

            TestSecurityUtil.setSecurityContext(BUYER_ID);
            try {
                assertThatThrownBy(() -> commandHandler.handle(command))
                    .isInstanceOf(OrderDomainException.class);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("非认领方尝试支付时抛出异常")
        void handle_payOrder_notOwner() {
            PayOrderCommand command = PayOrderCommand.builder()
                .orderId(ORDER_ID)
                .build();

            OrderAggregate aggregate = createPendingPaymentAggregate();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(999L);
            try {
                assertThatThrownBy(() -> commandHandler.handle(command))
                    .isInstanceOf(Exception.class);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("handle(CancelOrderCommand)")
    class CancelOrderTests {

        @Test
        @DisplayName("正常取消订单")
        void handle_cancelOrder_success() {
            CancelOrderCommand command = CancelOrderCommand.builder()
                .orderId(ORDER_ID)
                .reason("不想要了")
                .build();

            OrderAggregate aggregate = createPendingPaymentAggregate();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(BUYER_ID);
            try {
                commandHandler.handle(command);

                verify(orderRepository).update(any(OrderAggregate.class));
                verify(domainEventPublisher).publish(any(OrderCancelledEvent.class));
                verify(orderCachePort).evictOrderCache(BUYER_ID, SELLER_ID);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("handle(ShipOrderCommand)")
    class ShipOrderTests {

        @Test
        @DisplayName("正常发货")
        void handle_shipOrder_success() {
            ShipOrderCommand command = ShipOrderCommand.builder()
                .orderId(ORDER_ID)
                .build();

            OrderAggregate aggregate = createPaidAggregate();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(SELLER_ID);
            try {
                commandHandler.handle(command);

                verify(orderRepository).update(any(OrderAggregate.class));
                verify(domainEventPublisher).publish(any(OrderShippedEvent.class));
                verify(orderCachePort).evictOrderCache(BUYER_ID, SELLER_ID);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("非资产方尝试发货时抛出异常")
        void handle_shipOrder_notSeller() {
            ShipOrderCommand command = ShipOrderCommand.builder()
                .orderId(ORDER_ID)
                .build();

            OrderAggregate aggregate = createPaidAggregate();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(999L);
            try {
                assertThatThrownBy(() -> commandHandler.handle(command))
                    .isInstanceOf(Exception.class);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("handle(ConfirmReceiptCommand)")
    class ConfirmReceiptTests {

        @Test
        @DisplayName("正常确认收货")
        void handle_confirmReceipt_success() {
            ConfirmReceiptCommand command = ConfirmReceiptCommand.builder()
                .orderId(ORDER_ID)
                .build();

            OrderAggregate aggregate = createShippedAggregate();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(BUYER_ID);
            try {
                commandHandler.handle(command);

                verify(orderRepository).update(any(OrderAggregate.class));
                verify(domainEventPublisher).publish(any(OrderCompletedEvent.class));
                verify(orderCachePort).evictOrderCache(BUYER_ID, SELLER_ID);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("handle(RefundOrderCommand)")
    class RefundOrderTests {

        @Test
        @DisplayName("正常退款")
        void handle_refundOrder_success() {
            RefundOrderCommand command = RefundOrderCommand.builder()
                .orderId(ORDER_ID)
                .reason("商品有问题")
                .build();

            OrderAggregate aggregate = createPaidAggregate();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(BUYER_ID);
            try {
                commandHandler.handle(command);

                verify(paymentGatewayPort).refundPayment(ORDER_ID, "商品有问题");
                verify(orderRepository).update(any(OrderAggregate.class));
                verify(orderCachePort).evictOrderCache(BUYER_ID, SELLER_ID);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("订单支付未完成时退款应抛出明确异常")
        void handle_refundOrder_unpaidPayment_throwsException() {
            RefundOrderCommand command = RefundOrderCommand.builder()
                .orderId(ORDER_ID)
                .reason("商品有问题")
                .build();

            OrderAggregate aggregate = createPaidButUnpaidPaymentAggregate();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(BUYER_ID);
            try {
                assertThatThrownBy(() -> commandHandler.handle(command))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("无法退款");
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    private List<OrderItem> itemForTest() {
        return List.of(OrderItem.builder()
                .id(1L)
                .productId(ProductId.of(PRODUCT_ID))
                .unitPrice(Money.of(new BigDecimal("99.99")))
                .quantity(1)
                .subtotal(Money.of(new BigDecimal("99.99")))
                .build());
    }

    private OrderAggregate createPendingPaymentAggregate() {
        return OrderAggregate.from(
            OrderId.of(ORDER_ID), OrderNo.of("ORD1"),
            UserId.of(BUYER_ID), UserId.of(SELLER_ID), itemForTest(),
            Money.of(new BigDecimal("99.99")), OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID,
            Address.of("地址"), Phone.of("13800138000"), "备注", null, null
        );
    }

    private OrderAggregate createPaidAggregate() {
        return OrderAggregate.from(
            OrderId.of(ORDER_ID), OrderNo.of("ORD1"),
            UserId.of(BUYER_ID), UserId.of(SELLER_ID), itemForTest(),
            Money.of(new BigDecimal("99.99")), OrderStatus.PAID, PaymentStatus.PAID,
            Address.of("地址"), Phone.of("13800138000"), "备注", null, null
        );
    }

    private OrderAggregate createShippedAggregate() {
        return OrderAggregate.from(
            OrderId.of(ORDER_ID), OrderNo.of("ORD1"),
            UserId.of(BUYER_ID), UserId.of(SELLER_ID), itemForTest(),
            Money.of(new BigDecimal("99.99")), OrderStatus.SHIPPED, PaymentStatus.PAID,
            Address.of("地址"), Phone.of("13800138000"), "备注", null, null
        );
    }

    private OrderAggregate createPaidButUnpaidPaymentAggregate() {
        return OrderAggregate.from(
            OrderId.of(ORDER_ID), OrderNo.of("ORD1"),
            UserId.of(BUYER_ID), UserId.of(SELLER_ID), itemForTest(),
            Money.of(new BigDecimal("99.99")), OrderStatus.PAID, PaymentStatus.UNPAID,
            Address.of("地址"), Phone.of("13800138000"), "备注", null, null
        );
    }
}
