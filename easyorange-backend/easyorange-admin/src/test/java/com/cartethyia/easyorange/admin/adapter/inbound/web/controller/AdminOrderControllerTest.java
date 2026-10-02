package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminOrderAssembler;
import com.cartethyia.easyorange.admin.application.service.AdminOrderAppService;
import com.cartethyia.easyorange.admin.domain.model.OrderDetailView;
import com.cartethyia.easyorange.admin.domain.model.OrderListView;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderItemDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderItemInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderStats;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderSummary;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.ProductInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserInfo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 后台订单接口契约测试 — 服务给 {@code domain} 读模型，assembler 用真身，锁的是最终 JSON。
 */
@WebMvcTest(AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AdminOrderAssembler.class)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminOrderAppService adminOrderService;

    private static OrderListView listView() {
        OrderSummary order = new OrderSummary(
                "1",
                "ORD001",
                "10",
                "20",
                BigDecimal.valueOf(199),
                "PENDING_PAYMENT",
                "待付款",
                "UNPAID",
                "未支付",
                LocalDateTime.of(2026, 5, 16, 10, 0));
        OrderItemInfo item = new OrderItemInfo("1", "100", 1, BigDecimal.valueOf(199));
        return new OrderListView(
                new OrderQueryResult(List.of(order), 1, 1, 20),
                Map.of(
                        "10", new UserInfo("10", "buyer1", "buyer1", null, null),
                        "20", new UserInfo("20", "seller1", "seller1", null, null)),
                Map.of("1", List.of(item)),
                Map.of("100", new ProductInfo("100", "Product1", BigDecimal.valueOf(199))));
    }

    @Test
    void listOrders_shouldReturnPaginatedOrders() throws Exception {
        when(adminOrderService.listOrders(any(OrderQueryCondition.class))).thenReturn(listView());

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.records[0].orderId").value("1"))
                .andExpect(jsonPath("$.data.records[0].orderNo").value("ORD001"))
                .andExpect(jsonPath("$.data.records[0].status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.records[0].buyerName").value("buyer1"))
                .andExpect(jsonPath("$.data.records[0].items[0].productName").value("Product1"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void listOrders_shouldMapDateRangeToWholeDays() throws Exception {
        when(adminOrderService.listOrders(any(OrderQueryCondition.class))).thenReturn(listView());

        mockMvc.perform(get("/api/admin/orders?startTime=2026-05-01&endTime=2026-05-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));

        var captor = ArgumentCaptor.forClass(OrderQueryCondition.class);
        verify(adminOrderService).listOrders(captor.capture());
        assertThat(captor.getValue().startTime()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
        assertThat(captor.getValue().endTime()).isEqualTo(LocalDateTime.of(2026, 5, 2, 23, 59, 59));
    }

    @Test
    void getOrderDetail_shouldReturnDetail() throws Exception {
        OrderDetail order = new OrderDetail(
                "1",
                "ORD001",
                "10",
                "20",
                List.of(new OrderItemDetail("item-1", "100", 1, BigDecimal.valueOf(199), BigDecimal.valueOf(199))),
                BigDecimal.valueOf(199),
                "PENDING_PAYMENT",
                "待付款",
                "UNPAID",
                "PAY20260516001",
                BigDecimal.valueOf(199),
                null,
                "北京市朝阳区xxx",
                "13800138000",
                null,
                null,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                null,
                LocalDateTime.of(2026, 5, 16, 10, 5),
                null,
                null,
                null);
        when(adminOrderService.getOrderDetail("1"))
                .thenReturn(new OrderDetailView(
                        order,
                        new UserInfo("10", "buyer1", "buyer1", "avatar1", "13800138000"),
                        new UserInfo("20", "seller1", "seller1", "avatar2", "13900139000"),
                        Map.of("100", new ProductInfo("100", "Product1", BigDecimal.valueOf(199)))));

        mockMvc.perform(get("/api/admin/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.orderId").value("1"))
                .andExpect(jsonPath("$.data.orderNo").value("ORD001"))
                .andExpect(jsonPath("$.data.buyer.userId").value("10"))
                .andExpect(jsonPath("$.data.seller.nickname").value("seller1"))
                // 行项曾以 products / name / price 返回，与前端声明的 items / productName / unitPrice
                // 字段名全不相同，orderData.items 恒为 undefined，数量与单价永不显示
                .andExpect(jsonPath("$.data.items[0].itemId").value("item-1"))
                .andExpect(jsonPath("$.data.items[0].productId").value("100"))
                .andExpect(jsonPath("$.data.items[0].productName").value("Product1"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(199))
                .andExpect(jsonPath("$.data.items[0].quantity").value(1))
                .andExpect(jsonPath("$.data.items[0].subtotal").value(199))
                // 支付与收货信息曾长期恒为 null：前端四块 UI 永远走空分支，这里锁住实际下发
                .andExpect(jsonPath("$.data.paymentNo").value("PAY20260516001"))
                .andExpect(jsonPath("$.data.paidAmount").value(199))
                .andExpect(jsonPath("$.data.payTime").value("2026-05-16T10:05:00"))
                .andExpect(jsonPath("$.data.shippingAddress.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.shippingAddress.detailAddress").value("北京市朝阳区xxx"));
    }

    @Test
    void getOrderStats_shouldReturnStats() throws Exception {
        when(adminOrderService.getOrderStats())
                .thenReturn(new OrderStats(
                        1000, 50, 200, 100, 150, 500, 30, 20, BigDecimal.valueOf(50000), BigDecimal.valueOf(3000)));

        mockMvc.perform(get("/api/admin/orders/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.totalOrders").value(1000))
                .andExpect(jsonPath("$.data.todayOrders").value(50))
                .andExpect(jsonPath("$.data.pendingPayment").value(200))
                .andExpect(jsonPath("$.data.completed").value(500));
    }

    @Test
    void cancelOrder_shouldSucceed() throws Exception {
        doNothing().when(adminOrderService).cancelOrder(eq("1"), eq("认领方申请取消"));

        mockMvc.perform(put("/api/admin/orders/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"认领方申请取消\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void cancelOrder_withoutReason_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/admin/orders/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forceComplete_shouldSucceed() throws Exception {
        doNothing().when(adminOrderService).forceComplete(eq("1"), eq("管理员强制完成"));

        mockMvc.perform(put("/api/admin/orders/1/force-complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"管理员强制完成\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void forceComplete_withoutReason_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/admin/orders/1/force-complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refundOrder_shouldSucceed() throws Exception {
        doNothing().when(adminOrderService).refundOrder(eq("1"), eq("商品质量问题退款"));

        mockMvc.perform(put("/api/admin/orders/1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"商品质量问题退款\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void refundOrder_withoutReason_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/admin/orders/1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
