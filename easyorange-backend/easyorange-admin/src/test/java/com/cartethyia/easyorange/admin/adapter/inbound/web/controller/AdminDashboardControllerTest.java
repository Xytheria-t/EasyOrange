package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ActivityResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.DashboardStatsResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.TrendResponse;
import com.cartethyia.easyorange.admin.service.AdminDashboardService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @Test
    void getStats_shouldReturnDashboardStats() throws Exception {
        var stats = DashboardStatsResponse.builder()
                .totalUsers(100L)
                .todayNewUsers(5L)
                .totalProducts(200L)
                .pendingProducts(10L)
                .totalOrders(300L)
                .todayOrders(15L)
                .totalRevenue(new BigDecimal("12345.60"))
                .build();
        when(adminDashboardService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.totalUsers").value(100))
                .andExpect(jsonPath("$.data.todayNewUsers").value(5))
                .andExpect(jsonPath("$.data.totalProducts").value(200))
                .andExpect(jsonPath("$.data.pendingProducts").value(10))
                .andExpect(jsonPath("$.data.totalOrders").value(300))
                .andExpect(jsonPath("$.data.todayOrders").value(15))
                .andExpect(jsonPath("$.data.totalRevenue").value(12345.60));
    }

    @Test
    void getTrend_shouldReturnTrendList() throws Exception {
        var trends = List.of(TrendResponse.builder()
                .month("2026-01")
                .users(10L)
                .products(5L)
                .orders(3L)
                .build());
        when(adminDashboardService.getTrend()).thenReturn(trends);

        mockMvc.perform(get("/api/admin/dashboard/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data[0].month").value("2026-01"))
                .andExpect(jsonPath("$.data[0].users").value(10));
    }

    @Test
    void getActivity_shouldReturnActivityList() throws Exception {
        var activities = List.of(ActivityResponse.builder()
                .time("2026-05-16 10:00")
                .text("新用户 test 完成注册")
                .type("user")
                .build());
        when(adminDashboardService.getRecentActivity()).thenReturn(activities);

        mockMvc.perform(get("/api/admin/dashboard/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data[0].type").value("user"))
                .andExpect(jsonPath("$.data[0].text").value("新用户 test 完成注册"));
    }

}
