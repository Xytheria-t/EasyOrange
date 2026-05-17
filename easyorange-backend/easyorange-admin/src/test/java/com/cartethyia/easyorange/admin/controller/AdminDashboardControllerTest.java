package com.cartethyia.easyorange.admin.controller;

import com.cartethyia.easyorange.admin.dto.response.ActivityVO;
import com.cartethyia.easyorange.admin.dto.response.DashboardStatsVO;
import com.cartethyia.easyorange.admin.dto.response.PendingItemsVO;
import com.cartethyia.easyorange.admin.dto.response.RecentProductVO;
import com.cartethyia.easyorange.admin.dto.response.RecentUserVO;
import com.cartethyia.easyorange.admin.dto.response.TopProductVO;
import com.cartethyia.easyorange.admin.dto.response.TrendVO;
import com.cartethyia.easyorange.admin.dto.response.UserActivityHeatmapVO;
import com.cartethyia.easyorange.admin.service.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @Test
    void getStats_shouldReturnDashboardStats() throws Exception {
        var stats = DashboardStatsVO.builder()
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
        var pending = PendingItemsVO.builder()
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
            RecentUserVO.builder().userId(1L).username("user1").nickname("User1").build(),
            RecentUserVO.builder().userId(2L).username("user2").nickname("User2").build()
        );
        when(adminDashboardService.getRecentUsers(10)).thenReturn(users);

        mockMvc.perform(get("/api/admin/dashboard/recent-users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].userId").value(1))
            .andExpect(jsonPath("$.data[1].username").value("user2"));
    }

    @Test
    void getRecentProducts_shouldReturnProductList() throws Exception {
        var products = List.of(
            RecentProductVO.builder().productId(1L).name("Product1").price(BigDecimal.valueOf(100)).build()
        );
        when(adminDashboardService.getRecentProducts(10)).thenReturn(products);

        mockMvc.perform(get("/api/admin/dashboard/recent-products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data[0].productId").value(1))
            .andExpect(jsonPath("$.data[0].name").value("Product1"));
    }

    @Test
    void getTrend_shouldReturnTrendList() throws Exception {
        var trends = List.of(
            TrendVO.builder().month("2026-01").users(10L).products(5L).orders(3L).build()
        );
        when(adminDashboardService.getTrend()).thenReturn(trends);

        mockMvc.perform(get("/api/admin/dashboard/trend"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data[0].month").value("2026-01"))
            .andExpect(jsonPath("$.data[0].users").value(10));
    }

    @Test
    void getActivity_shouldReturnActivityList() throws Exception {
        var activities = List.of(
            ActivityVO.builder().time("2026-05-16 10:00").text("新用户 test 完成注册").type("user").build()
        );
        when(adminDashboardService.getRecentActivity()).thenReturn(activities);

        mockMvc.perform(get("/api/admin/dashboard/activity"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data[0].type").value("user"))
            .andExpect(jsonPath("$.data[0].text").value("新用户 test 完成注册"));
    }

    @Test
    void getUserActivityHeatmap_shouldReturnHeatmapData() throws Exception {
        var heatmap = List.of(
            new UserActivityHeatmapVO(2, 10, 5L),
            new UserActivityHeatmapVO(3, 14, 8L)
        );
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
        var topProducts = List.of(
            TopProductVO.builder().productId(1L).name("Top1").viewCount(1000)
                .price(BigDecimal.valueOf(99)).status(1).statusDesc("上架").build()
        );
        when(adminDashboardService.getTopProducts(10)).thenReturn(topProducts);

        mockMvc.perform(get("/api/admin/dashboard/top-products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data[0].productId").value(1))
            .andExpect(jsonPath("$.data[0].viewCount").value(1000));
    }

    @Test
    void getTopProducts_withCustomLimit_shouldUseGivenLimit() throws Exception {
        var topProducts = List.of(
            TopProductVO.builder().productId(1L).name("Top1").viewCount(500).build()
        );
        when(adminDashboardService.getTopProducts(5)).thenReturn(topProducts);

        mockMvc.perform(get("/api/admin/dashboard/top-products?limit=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data[0].productId").value(1));
    }
}
