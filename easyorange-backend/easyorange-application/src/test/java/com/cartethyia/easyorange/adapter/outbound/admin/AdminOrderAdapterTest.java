package com.cartethyia.easyorange.adapter.outbound.admin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderItemMapper;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderMapper;
import com.cartethyia.easyorange.order.application.port.query.OrderQueryRepository;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.aggregate.OrderReconstructSpec;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import com.cartethyia.easyorange.order.domain.valueobject.Version;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOrderAdapter 单元测试")
class AdminOrderAdapterTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private OrderQueryRepository orderReadRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private AdminOrderAdapter adapter;

    private static final String ORDER_ID = "100";
    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";

    @BeforeEach
    void setUp() {
        adapter = new AdminOrderAdapter(
                orderMapper,
                orderItemMapper,
                productMapper,
                orderReadRepository,
                orderRepository,
                domainEventPublisher);
    }

    private Order order(OrderStatus status, PaymentStatus paymentStatus) {
        return Order.from(new OrderReconstructSpec(
                OrderId.of(ORDER_ID),
                OrderNo.of("ORD2026001"),
                UserId.of(BUYER_ID),
                UserId.of(SELLER_ID),
                List.of(),
                Money.of(new BigDecimal("99.99")),
                status,
                paymentStatus,
                Address.of("地址"),
                Phone.of("13800138000"),
                "备注",
                null,
                null,
                null,
                null,
                Version.INITIAL));
    }

    @Test
    @DisplayName("取消待付款订单走 cancel")
    void cancelOrder_pendingPayment_cancels() {
        when(orderRepository.findById(OrderId.of(ORDER_ID)))
                .thenReturn(Optional.of(order(OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID)));

        adapter.cancelOrder(ORDER_ID, "管理端取消");

        verify(orderRepository).update(any(Order.class));
    }

    @Test
    @DisplayName("取消已付款订单走 forceCancel")
    void cancelOrder_paid_forceCancels() {
        when(orderRepository.findById(OrderId.of(ORDER_ID)))
                .thenReturn(Optional.of(order(OrderStatus.PAID, PaymentStatus.PAID)));

        adapter.cancelOrder(ORDER_ID, "管理端取消");

        verify(orderRepository).update(any(Order.class));
    }

    @Test
    @DisplayName("已完成订单不允许取消")
    void cancelOrder_completed_throws() {
        when(orderRepository.findById(OrderId.of(ORDER_ID)))
                .thenReturn(Optional.of(order(OrderStatus.COMPLETED, PaymentStatus.PAID)));

        assertThatThrownBy(() -> adapter.cancelOrder(ORDER_ID, "取消"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许取消");
        verify(orderRepository, never()).update(any());
    }

    @Test
    @DisplayName("订单不存在抛出业务异常")
    void cancelOrder_notFound_throws() {
        when(orderRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.cancelOrder(ORDER_ID, "取消"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    @DisplayName("强制完成订单")
    void forceComplete_succeeds() {
        when(orderRepository.findById(OrderId.of(ORDER_ID)))
                .thenReturn(Optional.of(order(OrderStatus.SHIPPED, PaymentStatus.PAID)));

        adapter.forceComplete(ORDER_ID);

        verify(orderRepository).update(any(Order.class));
    }

    @Test
    @DisplayName("退款已付款订单")
    void refundOrder_succeeds() {
        when(orderRepository.findById(OrderId.of(ORDER_ID)))
                .thenReturn(Optional.of(order(OrderStatus.PAID, PaymentStatus.PAID)));

        adapter.refundOrder(ORDER_ID, "商品问题退款");

        verify(orderRepository).update(any(Order.class));
    }

    @Test
    @DisplayName("已取消订单无法退款")
    void refundOrder_cancelled_throws() {
        when(orderRepository.findById(OrderId.of(ORDER_ID)))
                .thenReturn(Optional.of(order(OrderStatus.CANCELLED, PaymentStatus.UNPAID)));

        assertThatThrownBy(() -> adapter.refundOrder(ORDER_ID, "退款"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无法退款");
    }
}
