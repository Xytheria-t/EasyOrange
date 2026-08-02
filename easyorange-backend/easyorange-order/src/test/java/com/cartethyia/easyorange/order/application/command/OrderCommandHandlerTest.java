package com.cartethyia.easyorange.order.application.command;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;

import com.cartethyia.easyorange.framework.util.TestSecurityUtil;

import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.application.service.OrderCreationService;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.cartethyia.easyorange.order.application.command.CreateOrderCommand.CreateOrderItem;
import static com.cartethyia.easyorange.order.domain.aggregate.OrderTestFixture.aReconstructSpec;

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
    private OrderCreationService orderCreationService;

    @Mock
    private PaymentGatewayPort paymentGatewayPort;

    @Mock
    private OrderCachePort orderCachePort;

    @InjectMocks
    private OrderCommandHandler commandHandler;

    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";
    private static final String ORDER_ID = "100";
    private static final String PRODUCT_ID = "200";

    @Nested
    @DisplayName("handle(CreateOrderCommand)")
    class CreateOrderTests {

        @Test
        @DisplayName("正常创建订单")
        void handle_createOrder_success() {
            CreateOrderCommand command = new CreateOrderCommand(
                    List.of(new CreateOrderItem(PRODUCT_ID, 1)),
                    "北京市朝阳区", "13800138000", "尽快发货", null
            );

            CreateOrderResult expectedResult = new CreateOrderResult(ORDER_ID, "ORD123");
            when(orderCreationService.execute(command)).thenReturn(expectedResult);

            CreateOrderResult result = commandHandler.handle(command);

            assertThat(result.orderId()).isEqualTo(ORDER_ID);
            assertThat(result.orderNo()).isEqualTo("ORD123");
            verify(orderCreationService).execute(command);
        }
    }

    @Nested
    @DisplayName("handle(PayOrderCommand)")
    class PayOrderTests {

        @Test
        @DisplayName("正常支付订单")
        void handle_payOrder_success() {
            PayOrderCommand command = new PayOrderCommand(ORDER_ID);

            Order aggregate = createPendingPaymentOrder();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(BUYER_ID);
            try {
                commandHandler.handle(command);

                verify(orderRepository).update(any(Order.class));
                verify(domainEventPublisher).publish(any(OrderPaidEvent.class));
                verify(orderCachePort).evictOrderCache(BUYER_ID, SELLER_ID);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("订单不存在时抛出异常")
        void handle_payOrder_orderNotFound() {
            PayOrderCommand command = new PayOrderCommand(ORDER_ID);

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
            PayOrderCommand command = new PayOrderCommand(ORDER_ID);

            Order aggregate = createPendingPaymentOrder();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext("999");
            try {
                assertThatThrownBy(() -> commandHandler.handle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(OrderResultCode.ORDER_NOT_OWNER.getCode());
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
            CancelOrderCommand command = new CancelOrderCommand(ORDER_ID, "不想要了");

            Order aggregate = createPendingPaymentOrder();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(BUYER_ID);
            try {
                commandHandler.handle(command);

                verify(orderRepository).update(any(Order.class));
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
            ShipOrderCommand command = new ShipOrderCommand(ORDER_ID);

            Order aggregate = createPaidAggregate();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(SELLER_ID);
            try {
                commandHandler.handle(command);

                verify(orderRepository).update(any(Order.class));
                verify(domainEventPublisher).publish(any(OrderShippedEvent.class));
                verify(orderCachePort).evictOrderCache(BUYER_ID, SELLER_ID);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("非资产方尝试发货时抛出异常")
        void handle_shipOrder_notSeller() {
            ShipOrderCommand command = new ShipOrderCommand(ORDER_ID);

            Order aggregate = createPaidAggregate();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext("999");
            try {
                assertThatThrownBy(() -> commandHandler.handle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(OrderResultCode.ORDER_NOT_OWNER.getCode());
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
            ConfirmReceiptCommand command = new ConfirmReceiptCommand(ORDER_ID);

            Order aggregate = createShippedAggregate();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(BUYER_ID);
            try {
                commandHandler.handle(command);

                verify(orderRepository).update(any(Order.class));
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
            RefundOrderCommand command = new RefundOrderCommand(ORDER_ID, "商品有问题");

            Order aggregate = createPaidAggregate();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(BUYER_ID);
            try {
                commandHandler.handle(command);

                verify(paymentGatewayPort).refundPayment(ORDER_ID, "商品有问题");
                verify(orderRepository).update(any(Order.class));
                verify(orderCachePort).evictOrderCache(BUYER_ID, SELLER_ID);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("订单支付未完成时退款应抛出明确异常")
        void handle_refundOrder_unpaidPayment_throwsException() {
            RefundOrderCommand command = new RefundOrderCommand(ORDER_ID, "商品有问题");

            Order aggregate = createPaidButUnpaidOrder();
            when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(BUYER_ID);
            try {
                assertThatThrownBy(() -> commandHandler.handle(command))
                    .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(OrderResultCode.ORDER_CANNOT_REFUND.getCode());
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    // ==================== Aggregate fixtures ====================

    private Order createPendingPaymentOrder() {
        return Order.from(aReconstructSpec()
                .id(ORDER_ID)
                .orderNo("ORD" + ORDER_ID)
                .status(OrderStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.UNPAID)
                .build());
    }

    private Order createPaidAggregate() {
        return Order.from(aReconstructSpec()
                .id(ORDER_ID)
                .orderNo("ORD" + ORDER_ID)
                .status(OrderStatus.PAID)
                .paymentStatus(PaymentStatus.PAID)
                .build());
    }

    private Order createShippedAggregate() {
        return Order.from(aReconstructSpec()
                .id(ORDER_ID)
                .orderNo("ORD" + ORDER_ID)
                .status(OrderStatus.SHIPPED)
                .paymentStatus(PaymentStatus.PAID)
                .build());
    }

    private Order createPaidButUnpaidOrder() {
        return Order.from(aReconstructSpec()
                .id(ORDER_ID)
                .orderNo("ORD" + ORDER_ID)
                .status(OrderStatus.PAID)
                .paymentStatus(PaymentStatus.UNPAID)
                .build());
    }
}
