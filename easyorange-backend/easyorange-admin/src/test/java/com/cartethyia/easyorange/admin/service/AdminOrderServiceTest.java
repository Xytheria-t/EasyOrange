package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminOrderQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderDetailResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.OrderStatsResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderItemInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderSummary;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.ProductInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort.UserInfo;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.aggregate.OrderReconstructSpec;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOrderService 单元测试")
class AdminOrderServiceTest {

    @Mock
    private AdminOrderQueryPort adminOrderQueryPort;

    @Mock
    private AdminUserQueryPort adminUserQueryPort;

    @Mock
    private OrderReadRepository orderReadRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AdminOrderService orderService;

    private static final String ORDER_ID = "100";
    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";
    private static final String PRODUCT_ID = "200";

    private OrderSummary createOrderSummary(String status) {
        return new OrderSummary(
            ORDER_ID, "ORD2026001", BUYER_ID, SELLER_ID,
            new BigDecimal("99.99"), status, "待支付", PaymentStatus.UNPAID.getCode(), "未支付",
            LocalDateTime.now()
        );
    }

    private OrderReadModel createReadModel(String status) {
        return new OrderReadModel(
                ORDER_ID, "ORD2026001", BUYER_ID, SELLER_ID,
                List.of(new OrderItemReadModel("1", PRODUCT_ID, "{}", new BigDecimal("99.99"), 1, new BigDecimal("99.99"))),
                new BigDecimal("99.99"), status, "待支付", PaymentStatus.UNPAID.getCode(),
                "地址", "13800138000", "备注", null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    /**
     * 构建订单聚合根 — 使用 OrderReconstructSpec 替代已废弃的 fromRaw。
     */
    private OrderAggregate rebuildAggregate(OrderStatus status, PaymentStatus paymentStatus,
                                             String cancelReason, java.time.LocalDateTime cancelTime) {
        return OrderAggregate.from(new OrderReconstructSpec(
                OrderId.of(ORDER_ID), OrderNo.of("ORD2026001"),
                UserId.of(BUYER_ID), UserId.of(SELLER_ID),
                List.of(),
                Money.of(new BigDecimal("99.99")),
                status, paymentStatus,
                Address.of("地址"), Phone.of("13800138000"),
                "备注", cancelReason, cancelTime
        ));
    }

    @Nested
    @DisplayName("listOrders")
    class ListOrdersTests {

        @Test
        @DisplayName("分页查询订单列表")
        void listOrders_returnsPage() {
            AdminOrderQueryRequest request = new AdminOrderQueryRequest();
            OrderSummary order = createOrderSummary(OrderStatus.PENDING_PAYMENT.getCode());

            when(adminOrderQueryPort.queryOrders(any(OrderQueryCondition.class)))
                    .thenReturn(new OrderQueryResult(List.of(order), 1, 1, 20));
            when(adminUserQueryPort.getUserInfos(anyList()))
                    .thenReturn(Map.of(
                        BUYER_ID, new UserInfo(BUYER_ID, "buyer", "认领方", null, null),
                        SELLER_ID, new UserInfo(SELLER_ID, "seller", "资产方", null, null)
                    ));
            when(adminOrderQueryPort.getOrderItems(anyList()))
                    .thenReturn(Map.of(ORDER_ID, List.of(
                        new OrderItemInfo(ORDER_ID, PRODUCT_ID, 1, new BigDecimal("99.99"))
                    )));
            when(adminOrderQueryPort.getProducts(anyList()))
                    .thenReturn(Map.of(PRODUCT_ID, new ProductInfo(PRODUCT_ID, "测试商品", new BigDecimal("99.99"))));

            PageResult<AdminOrderResponse> result = orderService.listOrders(request);

            assertThat(result.records()).hasSize(1);
            assertThat(result.total()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getOrderDetail")
    class GetOrderDetailTests {

        @Test
        @DisplayName("获取订单详情成功")
        void getOrderDetail_success() {
            OrderReadModel model = createReadModel(OrderStatus.PENDING_PAYMENT.getCode());
            when(orderReadRepository.findById(new OrderId(ORDER_ID))).thenReturn(Optional.of(model));
            when(adminUserQueryPort.getUserInfo(BUYER_ID))
                    .thenReturn(new UserInfo(BUYER_ID, "buyer", "认领方", null, null));
            when(adminUserQueryPort.getUserInfo(SELLER_ID))
                    .thenReturn(new UserInfo(SELLER_ID, "seller", "资产方", null, null));
            when(adminOrderQueryPort.getProducts(anyList()))
                    .thenReturn(Map.of(PRODUCT_ID, new ProductInfo(PRODUCT_ID, "测试商品", new BigDecimal("99.99"))));

            AdminOrderDetailResponse detail = orderService.getOrderDetail(ORDER_ID);

            assertThat(detail).isNotNull();
            assertThat(detail.orderId()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("订单不存在时抛出异常")
        void getOrderDetail_notFound_throws() {
            when(orderReadRepository.findById(new OrderId(ORDER_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderDetail(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单不存在");
        }
    }

    @Nested
    @DisplayName("getOrderStats / cancelOrder / forceComplete / refundOrder")
    class OrderOperationsTests {

        @Test
        @DisplayName("获取订单统计")
        void getOrderStats_returnsStats() {
            when(orderReadRepository.countByStatus(null)).thenReturn(100L);
            when(orderReadRepository.countByStatus(OrderStatus.PENDING_PAYMENT.getCode())).thenReturn(20L);
            when(orderReadRepository.countByStatus(OrderStatus.PAID.getCode())).thenReturn(30L);
            when(orderReadRepository.countByStatus(OrderStatus.SHIPPED.getCode())).thenReturn(15L);
            when(orderReadRepository.countByStatus(OrderStatus.COMPLETED.getCode())).thenReturn(25L);
            when(orderReadRepository.countByStatus(OrderStatus.CANCELLED.getCode())).thenReturn(5L);
            when(orderReadRepository.countByStatus(OrderStatus.REFUNDED.getCode())).thenReturn(5L);
            when(adminOrderQueryPort.queryOrders(any(OrderQueryCondition.class)))
                    .thenReturn(new OrderQueryResult(List.of(), 10, 1, 20));

            OrderStatsResponse stats = orderService.getOrderStats();

            assertThat(stats.totalOrders()).isEqualTo(100);
            assertThat(stats.pendingPayment()).isEqualTo(20);
            assertThat(stats.toShip()).isEqualTo(30);
            assertThat(stats.completed()).isEqualTo(25);
        }

        @Test
        @DisplayName("取消待付款订单成功")
        void cancelOrder_success() {
            OrderAggregate aggregate = rebuildAggregate(
                    OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID, null, null);
            when(orderRepository.findById(new OrderId(ORDER_ID))).thenReturn(Optional.of(aggregate));

            orderService.cancelOrder(ORDER_ID, "认领方申请取消");

            verify(orderRepository).update(argThat(a -> a.status() == OrderStatus.CANCELLED));
        }

        @Test
        @DisplayName("取消不存在的订单抛出异常")
        void cancelOrder_notFound_throws() {
            when(orderRepository.findById(new OrderId(ORDER_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, "取消"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单不存在");
        }

        @Test
        @DisplayName("强制完成订单成功")
        void forceComplete_success() {
            OrderAggregate aggregate = rebuildAggregate(
                    OrderStatus.SHIPPED, PaymentStatus.PAID, null, null);
            when(orderRepository.findById(new OrderId(ORDER_ID))).thenReturn(Optional.of(aggregate));

            orderService.forceComplete(ORDER_ID, "强制完成");

            verify(orderRepository).update(argThat(a -> a.status() == OrderStatus.COMPLETED));
        }

        @Test
        @DisplayName("退款已付款订单成功")
        void refundOrder_success() {
            OrderAggregate aggregate = rebuildAggregate(
                    OrderStatus.PAID, PaymentStatus.PAID, null, null);
            when(orderRepository.findById(new OrderId(ORDER_ID))).thenReturn(Optional.of(aggregate));

            orderService.refundOrder(ORDER_ID, "商品问题退款");

            verify(orderRepository).update(argThat(a -> a.status() == OrderStatus.REFUNDED));
        }

        @Test
        @DisplayName("已取消订单无法退款")
        void refundOrder_cancelled_throws() {
            OrderAggregate aggregate = rebuildAggregate(
                    OrderStatus.CANCELLED, PaymentStatus.UNPAID, "已取消", LocalDateTime.now());
            when(orderRepository.findById(new OrderId(ORDER_ID))).thenReturn(Optional.of(aggregate));

            assertThatThrownBy(() -> orderService.refundOrder(ORDER_ID, "退款"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无法退款");
        }
    }
}