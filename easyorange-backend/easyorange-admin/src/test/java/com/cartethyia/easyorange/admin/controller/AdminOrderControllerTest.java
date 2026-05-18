package com.cartethyia.easyorange.admin.controller;

import com.cartethyia.easyorange.admin.dto.response.AdminOrderDetailResponse;
import com.cartethyia.easyorange.admin.dto.response.AdminOrderResponse;
import com.cartethyia.easyorange.admin.dto.response.OrderStatsResponse;
import com.cartethyia.easyorange.admin.service.AdminOrderService;
import com.cartethyia.easyorange.common.result.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminOrderService adminOrderService;

    @Test
    void listOrders_shouldReturnPaginatedOrders() throws Exception {
        var orders = List.of(
            new AdminOrderResponse(1L, "ORD001", 10L, "buyer1", 20L, "seller1",
                100L, "Product1", BigDecimal.valueOf(199), 1, "待付款", 0, "未支付",
                LocalDateTime.of(2026, 5, 16, 10, 0))
        );
        var pageResult = PageResult.of(orders, 1L, 1, 20);
        when(adminOrderService.listOrders(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.records[0].orderId").value(1))
            .andExpect(jsonPath("$.data.records[0].orderNo").value("ORD001"))
            .andExpect(jsonPath("$.data.records[0].status").value(1))
            .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void getOrderDetail_shouldReturnDetail() throws Exception {
        var detail = AdminOrderDetailResponse.builder()
            .orderId(1L).orderNo("ORD001")
            .buyer(new AdminOrderDetailResponse.BuyerInfo(10L, "buyer1", "avatar1", "13800138000"))
            .seller(new AdminOrderDetailResponse.SellerInfo(20L, "seller1", "avatar2", "13900139000"))
            .product(new AdminOrderDetailResponse.ProductInfo(100L, "Product1", "img.jpg", BigDecimal.valueOf(199)))
            .amount(BigDecimal.valueOf(199)).status(1).statusDesc("待付款").createTime(LocalDateTime.of(2026, 5, 16, 10, 0))
            .build();
        when(adminOrderService.getOrderDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.orderId").value(1))
            .andExpect(jsonPath("$.data.orderNo").value("ORD001"))
            .andExpect(jsonPath("$.data.buyer.userId").value(10))
            .andExpect(jsonPath("$.data.seller.nickname").value("seller1"))
            .andExpect(jsonPath("$.data.product.name").value("Product1"));
    }

    @Test
    void getOrderStats_shouldReturnStats() throws Exception {
        var stats = OrderStatsResponse.builder()
            .totalOrders(1000L).todayOrders(50L).pendingPayment(200L)
            .toShip(100L).toReceive(150L).completed(500L).cancelled(30L).refunded(20L)
            .totalRevenue(BigDecimal.valueOf(50000)).todayRevenue(BigDecimal.valueOf(3000))
            .build();
        when(adminOrderService.getOrderStats()).thenReturn(stats);

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
        doNothing().when(adminOrderService).cancelOrder(eq(1L), eq("买家申请取消"));

        mockMvc.perform(put("/api/admin/orders/1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"买家申请取消\"}"))
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
        doNothing().when(adminOrderService).forceComplete(eq(1L), eq("管理员强制完成"));

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
        doNothing().when(adminOrderService).refundOrder(eq(1L), eq("商品质量问题退款"));

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
