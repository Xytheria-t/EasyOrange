package com.cartethyia.easyorange.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ActivityResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.DashboardStatsResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.TrendResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminDashboardPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderStats;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserStats;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDashboardService 单元测试")
class AdminDashboardServiceTest {

    @Mock
    private AdminUserPort adminUserPort;

    @Mock
    private AdminDashboardPort adminDashboardPort;

    @Mock
    private AdminOrderPort adminOrderPort;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AdminDashboardService dashboardService;


    @Nested
    @DisplayName("getDashboardStats")
    class GetDashboardStatsTests {

        @Test
        @DisplayName("获取仪表盘统计数据")
        void getDashboardStats_returnsStats() {
            when(adminUserPort.getUserStats()).thenReturn(new UserStats(100, 5));
            when(adminDashboardPort.getProductStats()).thenReturn(new AdminDashboardPort.ProductStats(200, 10));
            when(adminOrderPort.getOrderStats())
                    .thenReturn(new OrderStats(
                            300, 5, 0, 0, 0, 0, 0, 0, new BigDecimal("12345.60"), new BigDecimal("88.00")));

            DashboardStatsResponse stats = dashboardService.getDashboardStats();

            assertThat(stats.getTotalUsers()).isEqualTo(100);
            assertThat(stats.getTodayNewUsers()).isEqualTo(5);
            assertThat(stats.getTotalProducts()).isEqualTo(200);
            assertThat(stats.getPendingProducts()).isEqualTo(10);
            assertThat(stats.getTotalOrders()).isEqualTo(300);
            assertThat(stats.getTodayOrders()).isEqualTo(5);
            assertThat(stats.getTotalRevenue()).isEqualByComparingTo("12345.60");
        }
    }

    @Nested
    @DisplayName("getTrend")
    class GetTrendTests {

        @Test
        @DisplayName("获取月度趋势数据")
        void getTrend_returnsTrendData() {
            Map<String, Object> userRow = new LinkedHashMap<>();
            userRow.put("month", "2026-05");
            userRow.put("cnt", 10L);
            when(jdbcTemplate.queryForList(anyString(), any(LocalDateTime.class)))
                    .thenReturn(List.of(userRow))
                    .thenReturn(List.of())
                    .thenReturn(List.of());

            List<TrendResponse> trend = dashboardService.getTrend();

            assertThat(trend).isNotEmpty();
            TrendResponse last = trend.get(trend.size() - 1);
            assertThat(last.getMonth()).startsWith("2026-0");
        }
    }

    @Nested
    @DisplayName("getRecentActivity")
    class GetRecentActivityTests {

        @Test
        @DisplayName("获取最近动态（无数据时返回空列表）")
        void getRecentActivity_noData_returnsEmptyList() {
            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

            List<ActivityResponse> activities = dashboardService.getRecentActivity();

            assertThat(activities).isEmpty();
        }

        @Test
        @DisplayName("获取最近动态（有数据时返回合并列表）")
        void getRecentActivity_withData_returnsMergedList() {
            Map<String, Object> userRow = new HashMap<>();
            userRow.put("user_id", "user-1");
            userRow.put("nick_name", "张三");
            userRow.put("create_time", Timestamp.valueOf(LocalDateTime.now()));

            Map<String, Object> productRow = new HashMap<>();
            productRow.put("id", 1L);
            productRow.put("name", "高等数学教材");
            productRow.put("create_time", Timestamp.valueOf(LocalDateTime.now()));

            when(jdbcTemplate.queryForList(anyString()))
                    .thenReturn(List.of(userRow))
                    .thenReturn(List.of(productRow))
                    .thenReturn(List.of());

            List<ActivityResponse> activities = dashboardService.getRecentActivity();

            assertThat(activities).hasSize(2);
            assertThat(activities.get(0).getText()).contains("张三");
            assertThat(activities.get(1).getText()).contains("高等数学教材");
        }
    }

}
