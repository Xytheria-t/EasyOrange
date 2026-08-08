package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ActivityResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.DashboardStatsResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.PendingItemsResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.RecentProductResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.RecentUserResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.TopProductResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.TrendResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.UserActivityHeatmapResponse;
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
                .totalRevenue(0L)
                .pendingReports(3L)
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
                .andExpect(jsonPath("$.data.pendingReports").value(3));
    }

    @Test
    void getPendingItems_shouldReturnPendingItems() throws Exception {
        var pending = PendingItemsResponse.builder()
                .pendingReports(3L)
                .pendingOrders(5L)
                .pendingProducts(10L)
                .recentReports(List.of())
                .build();
        when(adminDashboardService.getPendingItems()).thenReturn(pending);

        mockMvc.perform(get("/api/admin/dashboard/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.pendingReports").value(3))
                .andExpect(jsonPath("$.data.pendingOrders").value(5))
                .andExpect(jsonPath("$.data.pendingProducts").value(10))
                .andExpect(jsonPath("$.data.recentReports").isArray());
    }

    @Test
    void getRecentUsers_shouldReturnUserList() throws Exception {
        var users = List.of(
                RecentUserResponse.builder()
                        .userId("1")
                        .username("user1")
                        .nickname("User1")
                        .build(),
                RecentUserResponse.builder()
                        .userId("2")
                        .username("user2")
                        .nickname("User2")
                        .build());
        when(adminDashboardService.getRecentUsers(10)).thenReturn(users);

        mockMvc.perform(get("/api/admin/dashboard/recent-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].userId").value("1"))
                .andExpect(jsonPath("$.data[1].username").value("user2"));
    }

    @Test
    void getRecentProducts_shouldReturnProductList() throws Exception {
        var products = List.of(RecentProductResponse.builder()
                .productId("1")
                .name("Product1")
                .price(BigDecimal.valueOf(100))
                .build());
        when(adminDashboardService.getRecentProducts(10)).thenReturn(products);

        mockMvc.perform(get("/api/admin/dashboard/recent-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data[0].productId").value("1"))
                .andExpect(jsonPath("$.data[0].name").value("Product1"));
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

    @Test
    void getUserActivityHeatmap_shouldReturnHeatmapData() throws Exception {
        var heatmap = List.of(new UserActivityHeatmapResponse(2, 10, 5L), new UserActivityHeatmapResponse(3, 14, 8L));
        when(adminDashboardService.getUserActivityHeatmap()).thenReturn(heatmap);

        mockMvc.perform(get("/api/admin/dashboard/user-activity-heatmap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data[0].dayOfWeek").value(2))
                .andExpect(jsonPath("$.data[0].hour").value(10))
                .andExpect(jsonPath("$.data[0].count").value(5));
    }

    @Test
    void getTopProducts_shouldReturnTopProducts() throws Exception {
        var topProducts = List.of(TopProductResponse.builder()
                .productId("1")
                .name("Top1")
                .viewCount(1000)
                .price(BigDecimal.valueOf(99))
                .status("ONLINE")
                .statusDesc("上架")
                .build());
        when(adminDashboardService.getTopProducts(10)).thenReturn(topProducts);

        mockMvc.perform(get("/api/admin/dashboard/top-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data[0].productId").value("1"))
                .andExpect(jsonPath("$.data[0].viewCount").value(1000));
    }

    @Test
    void getTopProducts_withCustomLimit_shouldUseGivenLimit() throws Exception {
        var topProducts = List.of(TopProductResponse.builder()
                .productId("1")
                .name("Top1")
                .viewCount(500)
                .build());
        when(adminDashboardService.getTopProducts(5)).thenReturn(topProducts);

        mockMvc.perform(get("/api/admin/dashboard/top-products?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data[0].productId").value("1"));
    }
}
