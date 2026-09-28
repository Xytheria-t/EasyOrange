package com.cartethyia.easyorange.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.domain.exception.AdminDomainException;
import com.cartethyia.easyorange.admin.domain.model.OrderDetailView;
import com.cartethyia.easyorange.admin.domain.model.OrderListView;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderItemDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderItemInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderStats;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderSummary;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.ProductInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserInfo;
import com.cartethyia.easyorange.common.exception.BusinessException;
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

/**
 * 订单服务测试 — 断言的是**编排结果**（这一页补齐了哪些关联数据），字段命名由 assembler 的测试负责。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOrderAppService 单元测试")
class AdminOrderAppServiceTest {

    @Mock
    private AdminOrderPort adminOrderPort;

    @Mock
    private AdminUserPort adminUserPort;

    @InjectMocks
    private AdminOrderAppService orderService;

    private static final String ORDER_ID = "100";
    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";
    private static final String PRODUCT_ID = "200";

    private static final OrderQueryCondition CONDITION =
            new OrderQueryCondition(null, null, null, null, null, null, null, 1, 20);

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
                null,
                null,
                null);
    }

    @Nested
    @DisplayName("listOrders")
    class ListOrdersTests {

        @Test
        @DisplayName("分页查询订单列表")
        void listOrders_returnsPage() {
            OrderSummary order = createOrderSummary("PENDING_PAYMENT");

            when(adminOrderPort.queryOrders(CONDITION)).thenReturn(new OrderQueryResult(List.of(order), 1, 1, 20));
            when(adminUserPort.getUserInfos(anyList()))
                    .thenReturn(Map.of(
                            BUYER_ID, new UserInfo(BUYER_ID, "buyer", "认领方", null, null),
                            SELLER_ID, new UserInfo(SELLER_ID, "seller", "资产方", null, null)));
            when(adminOrderPort.getOrderItems(anyList()))
                    .thenReturn(Map.of(
                            ORDER_ID, List.of(new OrderItemInfo(ORDER_ID, PRODUCT_ID, 1, new BigDecimal("99.99")))));
            when(adminOrderPort.getProducts(anyList()))
                    .thenReturn(Map.of(PRODUCT_ID, new ProductInfo(PRODUCT_ID, "测试商品", new BigDecimal("99.99"))));

            OrderListView view = orderService.listOrders(CONDITION);

            assertThat(view.page().total()).isEqualTo(1);
            assertThat(view.page().pageNum()).isEqualTo(1);
            assertThat(view.users().get(BUYER_ID).nickName()).isEqualTo("认领方");
            assertThat(view.items().get(ORDER_ID)).hasSize(1);
            assertThat(view.products().get(PRODUCT_ID).name()).isEqualTo("测试商品");
        }

        @Test
        @DisplayName("用户 / 商品查不到时补空表，订单本身照常返回")
        void listOrders_missingRelatedData_returnsEmptyMaps() {
            OrderSummary order = createOrderSummary("PENDING_PAYMENT");
            when(adminOrderPort.queryOrders(CONDITION)).thenReturn(new OrderQueryResult(List.of(order), 1, 1, 20));
            when(adminUserPort.getUserInfos(anyList())).thenReturn(Map.of());
            when(adminOrderPort.getOrderItems(anyList())).thenReturn(Map.of());
            when(adminOrderPort.getProducts(anyList())).thenReturn(Map.of());

            OrderListView view = orderService.listOrders(CONDITION);

            assertThat(view.page().records()).hasSize(1);
            assertThat(view.users()).isEmpty();
            assertThat(view.items()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getOrderDetail")
    class GetOrderDetailTests {

        @Test
        @DisplayName("获取订单详情成功")
        void getOrderDetail_success() {
            when(adminOrderPort.getOrderDetail(ORDER_ID)).thenReturn(createOrderDetail("PENDING_PAYMENT"));
            when(adminUserPort.getUserInfo(BUYER_ID)).thenReturn(new UserInfo(BUYER_ID, "buyer", "认领方", null, null));
            when(adminUserPort.getUserInfo(SELLER_ID)).thenReturn(new UserInfo(SELLER_ID, "seller", "资产方", null, null));
            when(adminOrderPort.getProducts(anyList()))
                    .thenReturn(Map.of(PRODUCT_ID, new ProductInfo(PRODUCT_ID, "测试商品", new BigDecimal("99.99"))));

            OrderDetailView view = orderService.getOrderDetail(ORDER_ID);

            assertThat(view.order().id()).isEqualTo(ORDER_ID);
            assertThat(view.buyer().nickName()).isEqualTo("认领方");
            assertThat(view.products()).containsKey(PRODUCT_ID);
        }

        @Test
        @DisplayName("订单不存在时抛出异常")
        void getOrderDetail_notFound_throws() {
            when(adminOrderPort.getOrderDetail(ORDER_ID)).thenReturn(null);

            assertThatThrownBy(() -> orderService.getOrderDetail(ORDER_ID))
                    .isInstanceOf(AdminDomainException.class)
                    .hasMessageContaining("订单不存在");
        }

        @Test
        @DisplayName("买/卖/商品缺失时原样返回 null，由展示层兜底")
        void getOrderDetail_nullBuyerSellerProduct() {
            when(adminOrderPort.getOrderDetail(ORDER_ID)).thenReturn(createOrderDetail("PENDING_PAYMENT"));
            when(adminUserPort.getUserInfo(BUYER_ID)).thenReturn(null);
            when(adminUserPort.getUserInfo(SELLER_ID)).thenReturn(null);
            when(adminOrderPort.getProducts(anyList())).thenReturn(Map.of());

            OrderDetailView view = orderService.getOrderDetail(ORDER_ID);

            assertThat(view.order().items()).hasSize(1);
            assertThat(view.buyer()).isNull();
            assertThat(view.seller()).isNull();
        }
    }

    @Nested
    @DisplayName("getOrderStats / cancelOrder / forceComplete / refundOrder")
    class OrderOperationsTests {

        @Test
        @DisplayName("获取订单统计")
        void getOrderStats_returnsStats() {
            when(adminOrderPort.getOrderStats())
                    .thenReturn(new OrderStats(
                            100, 10, 20, 30, 15, 25, 5, 5, new BigDecimal("5000.00"), new BigDecimal("200.00")));

            OrderStats stats = orderService.getOrderStats();

            assertThat(stats.totalOrders()).isEqualTo(100);
            assertThat(stats.todayOrders()).isEqualTo(10);
            assertThat(stats.pendingPayment()).isEqualTo(20);
            assertThat(stats.toShip()).isEqualTo(30);
            assertThat(stats.toReceive()).isEqualTo(15);
            assertThat(stats.completed()).isEqualTo(25);
            assertThat(stats.cancelled()).isEqualTo(5);
            assertThat(stats.refunded()).isEqualTo(5);
            assertThat(stats.totalRevenue()).isEqualByComparingTo("5000.00");
            assertThat(stats.todayRevenue()).isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("取消订单委托端口")
        void cancelOrder_delegatesToPort() {
            orderService.cancelOrder(ORDER_ID, "认领方申请取消");

            verify(adminOrderPort).cancelOrder(ORDER_ID, "认领方申请取消");
        }

        @Test
        @DisplayName("端口抛出业务异常向上传播")
        void cancelOrder_portThrows_propagates() {
            doThrow(BusinessException.of("订单不存在")).when(adminOrderPort).cancelOrder(ORDER_ID, "取消");

            // 端口抛什么就上抛什么：服务层不把下游异常换一种类型，调用方才认得出错误码
            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, "取消"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单不存在");
        }

        @Test
        @DisplayName("强制完成订单委托端口（原因不落库，不下发）")
        void forceComplete_delegatesToPort() {
            orderService.forceComplete(ORDER_ID, "强制完成");

            verify(adminOrderPort).forceComplete(ORDER_ID);
        }

        @Test
        @DisplayName("退款订单委托端口")
        void refundOrder_delegatesToPort() {
            orderService.refundOrder(ORDER_ID, "商品问题退款");

            verify(adminOrderPort).refundOrder(ORDER_ID, "商品问题退款");
        }
    }
}
