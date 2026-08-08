package com.cartethyia.easyorange.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminOrderQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderDetailResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.OrderStatsResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderItemDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderItemInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderStats;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderSummary;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.ProductInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort.UserInfo;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOrderService 单元测试")
class AdminOrderServiceTest {

    @Mock
    private AdminOrderQueryPort adminOrderQueryPort;

    @Mock
    private AdminUserQueryPort adminUserQueryPort;

    @InjectMocks
    private AdminOrderService orderService;

    private static final String ORDER_ID = "100";
    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";
    private static final String PRODUCT_ID = "200";

    private OrderSummary createOrderSummary(String status) {
        return new OrderSummary(
                ORDER_ID,
                "ORD2026001",
                BUYER_ID,
                SELLER_ID,
                new BigDecimal("99.99"),
                status,
                "待支付",
                "UNPAID",
                "未支付",
                LocalDateTime.now());
    }

    private OrderDetail createOrderDetail(String status) {
        return new OrderDetail(
                ORDER_ID,
                "ORD2026001",
                BUYER_ID,
                SELLER_ID,
                List.of(new OrderItemDetail(PRODUCT_ID, 1, new BigDecimal("99.99"))),
                new BigDecimal("99.99"),
                status,
                "待支付",
                "UNPAID",
                "备注",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null);
    }

    @Nested
    @DisplayName("listOrders")
    class ListOrdersTests {

        @Test
        @DisplayName("分页查询订单列表")
        void listOrders_returnsPage() {
            AdminOrderQueryRequest request = new AdminOrderQueryRequest();
            OrderSummary order = createOrderSummary("PENDING_PAYMENT");

            when(adminOrderQueryPort.queryOrders(any(OrderQueryCondition.class)))
                    .thenReturn(new OrderQueryResult(List.of(order), 1, 1, 20));
            when(adminUserQueryPort.getUserInfos(anyList()))
                    .thenReturn(Map.of(
                            BUYER_ID, new UserInfo(BUYER_ID, "buyer", "认领方", null, null),
                            SELLER_ID, new UserInfo(SELLER_ID, "seller", "资产方", null, null)));
            when(adminOrderQueryPort.getOrderItems(anyList()))
                    .thenReturn(Map.of(
                            ORDER_ID, List.of(new OrderItemInfo(ORDER_ID, PRODUCT_ID, 1, new BigDecimal("99.99")))));
            when(adminOrderQueryPort.getProducts(anyList()))
                    .thenReturn(Map.of(PRODUCT_ID, new ProductInfo(PRODUCT_ID, "测试商品", new BigDecimal("99.99"))));

            PageResult<AdminOrderResponse> result = orderService.listOrders(request);

            assertThat(result.records()).hasSize(1);
            assertThat(result.total()).isEqualTo(1);
            assertThat(result.records().get(0).buyerName()).isEqualTo("认领方");
        }

        @Test
        @DisplayName("订单列表用户信息缺失时返回空昵称")
        void listOrders_missingUserInfo_returnsNullNickname() {
            AdminOrderQueryRequest request = new AdminOrderQueryRequest();
            OrderSummary order = createOrderSummary("PENDING_PAYMENT");
            when(adminOrderQueryPort.queryOrders(any(OrderQueryCondition.class)))
                    .thenReturn(new OrderQueryResult(List.of(order), 1, 1, 20));
            when(adminUserQueryPort.getUserInfos(anyList())).thenReturn(Map.of());
            when(adminOrderQueryPort.getOrderItems(anyList())).thenReturn(Map.of());
            when(adminOrderQueryPort.getProducts(anyList())).thenReturn(Map.of());

            PageResult<AdminOrderResponse> result = orderService.listOrders(request);

            assertThat(result.records()).hasSize(1);
            assertThat(result.records().get(0).buyerName()).isNull();
            assertThat(result.records().get(0).sellerName()).isNull();
        }

        @Test
        @DisplayName("非法时间格式回退为空")
        void listOrders_invalidTimes_returnsNullTimes() {
            AdminOrderQueryRequest request = new AdminOrderQueryRequest();
            request.setStartTime("invalid");
            request.setEndTime("invalid");
            OrderSummary order = createOrderSummary("PENDING_PAYMENT");
            when(adminOrderQueryPort.queryOrders(any(OrderQueryCondition.class)))
                    .thenReturn(new OrderQueryResult(List.of(order), 1, 1, 20));
            when(adminUserQueryPort.getUserInfos(anyList())).thenReturn(Map.of());
            when(adminOrderQueryPort.getOrderItems(anyList())).thenReturn(Map.of());
            when(adminOrderQueryPort.getProducts(anyList())).thenReturn(Map.of());

            PageResult<AdminOrderResponse> result = orderService.listOrders(request);

            assertThat(result.records()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getOrderDetail")
    class GetOrderDetailTests {

        @Test
        @DisplayName("获取订单详情成功")
        void getOrderDetail_success() {
            when(adminOrderQueryPort.getOrderDetail(ORDER_ID)).thenReturn(createOrderDetail("PENDING_PAYMENT"));
            when(adminUserQueryPort.getUserInfo(BUYER_ID))
                    .thenReturn(new UserInfo(BUYER_ID, "buyer", "认领方", null, null));
            when(adminUserQueryPort.getUserInfo(SELLER_ID))
                    .thenReturn(new UserInfo(SELLER_ID, "seller", "资产方", null, null));
            when(adminOrderQueryPort.getProducts(anyList()))
                    .thenReturn(Map.of(PRODUCT_ID, new ProductInfo(PRODUCT_ID, "测试商品", new BigDecimal("99.99"))));

            AdminOrderDetailResponse detail = orderService.getOrderDetail(ORDER_ID);

            assertThat(detail).isNotNull();
            assertThat(detail.orderId()).isEqualTo(ORDER_ID);
            assertThat(detail.buyer().nickname()).isEqualTo("认领方");
            assertThat(detail.products()).hasSize(1);
        }

        @Test
        @DisplayName("订单不存在时抛出异常")
        void getOrderDetail_notFound_throws() {
            when(adminOrderQueryPort.getOrderDetail(ORDER_ID)).thenReturn(null);

            assertThatThrownBy(() -> orderService.getOrderDetail(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单不存在");
        }

        @Test
        @DisplayName("订单详情中买/卖/商品缺失时返回空信息")
        void getOrderDetail_nullBuyerSellerProduct() {
            when(adminOrderQueryPort.getOrderDetail(ORDER_ID)).thenReturn(createOrderDetail("PENDING_PAYMENT"));
            when(adminUserQueryPort.getUserInfo(BUYER_ID)).thenReturn(null);
            when(adminUserQueryPort.getUserInfo(SELLER_ID)).thenReturn(null);
            when(adminOrderQueryPort.getProducts(anyList())).thenReturn(Map.of());

            AdminOrderDetailResponse detail = orderService.getOrderDetail(ORDER_ID);

            assertThat(detail).isNotNull();
            assertThat(detail.buyer().nickname()).isNull();
            assertThat(detail.seller().nickname()).isNull();
            assertThat(detail.products()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getOrderStats / cancelOrder / forceComplete / refundOrder")
    class OrderOperationsTests {

        @Test
        @DisplayName("获取订单统计")
        void getOrderStats_returnsStats() {
            when(adminOrderQueryPort.getOrderStats()).thenReturn(new OrderStats(100, 10, 20, 30, 15, 25, 5, 5));

            OrderStatsResponse stats = orderService.getOrderStats();

            assertThat(stats.totalOrders()).isEqualTo(100);
            assertThat(stats.todayOrders()).isEqualTo(10);
            assertThat(stats.pendingPayment()).isEqualTo(20);
            assertThat(stats.toShip()).isEqualTo(30);
            assertThat(stats.toReceive()).isEqualTo(15);
            assertThat(stats.completed()).isEqualTo(25);
            assertThat(stats.cancelled()).isEqualTo(5);
            assertThat(stats.refunded()).isEqualTo(5);
        }

        @Test
        @DisplayName("取消订单委托端口")
        void cancelOrder_delegatesToPort() {
            orderService.cancelOrder(ORDER_ID, "认领方申请取消");

            verify(adminOrderQueryPort).cancelOrder(ORDER_ID, "认领方申请取消");
        }

        @Test
        @DisplayName("端口抛出业务异常向上传播")
        void cancelOrder_portThrows_propagates() {
            doThrow(BusinessException.of("订单不存在")).when(adminOrderQueryPort).cancelOrder(ORDER_ID, "取消");

            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, "取消"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单不存在");
        }

        @Test
        @DisplayName("强制完成订单委托端口")
        void forceComplete_delegatesToPort() {
            orderService.forceComplete(ORDER_ID, "强制完成");

            verify(adminOrderQueryPort).forceComplete(ORDER_ID);
        }

        @Test
        @DisplayName("退款订单委托端口")
        void refundOrder_delegatesToPort() {
            orderService.refundOrder(ORDER_ID, "商品问题退款");

            verify(adminOrderQueryPort).refundOrder(ORDER_ID, "商品问题退款");
        }
    }
}
