package com.cartethyia.easyorange.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.domain.model.DashboardActivity;
import com.cartethyia.easyorange.admin.domain.model.DashboardStats;
import com.cartethyia.easyorange.admin.domain.model.RecentActivityRow;
import com.cartethyia.easyorange.admin.domain.model.TrendPoint;
import com.cartethyia.easyorange.admin.domain.port.AdminDashboardPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderStats;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserStats;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
@DisplayName("AdminDashboardAppService 单元测试")
class AdminDashboardAppServiceTest {

    @Mock
    private AdminUserPort adminUserPort;

    @Mock
    private AdminDashboardPort adminDashboardPort;

    @Mock
    private AdminOrderPort adminOrderPort;

    @Mock
    private AdminProductPort adminProductPort;

    @InjectMocks
    private AdminDashboardAppService dashboardService;

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

            DashboardStats stats = dashboardService.getDashboardStats();

            assertThat(stats.totalUsers()).isEqualTo(100);
            assertThat(stats.todayNewUsers()).isEqualTo(5);
            assertThat(stats.totalProducts()).isEqualTo(200);
            assertThat(stats.pendingProducts()).isEqualTo(10);
            assertThat(stats.totalOrders()).isEqualTo(300);
            assertThat(stats.todayOrders()).isEqualTo(5);
            assertThat(stats.totalRevenue()).isEqualByComparingTo("12345.60");
        }
    }

    @Nested
    @DisplayName("getTrend")
    class GetTrendTests {

        @Test
        @DisplayName("按自然月补零，缺数据的月份也要出现在折线上")
        void getTrend_fillsMissingMonths() {
            String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            when(adminUserPort.getCreateTrend(any())).thenReturn(Map.of(currentMonth, 7L));
            when(adminProductPort.getCreateTrend(any())).thenReturn(Map.of());
            when(adminOrderPort.getCreateTrend(any())).thenReturn(Map.of(currentMonth, 3L));

            List<TrendPoint> trend = dashboardService.getTrend();

            // 起点是「当月减 6 个月」，闭区间共 7 个月
            assertThat(trend).hasSize(7);
            TrendPoint last = trend.get(trend.size() - 1);
            assertThat(last.month()).isEqualTo(currentMonth);
            assertThat(last.users()).isEqualTo(7L);
            assertThat(last.orders()).isEqualTo(3L);
            assertThat(last.products()).isZero();
        }
    }

    @Nested
    @DisplayName("getActivityFeed")
    class GetActivityFeedTests {

        @Test
        @DisplayName("获取最近动态（无数据时返回空列表）")
        void getActivityFeed_noData_returnsEmptyList() {
            when(adminUserPort.findRecentRegistrations(anyInt())).thenReturn(List.of());
            when(adminProductPort.findRecentPublished(anyInt())).thenReturn(List.of());
            when(adminOrderPort.findRecentCreated(anyInt())).thenReturn(List.of());

            List<DashboardActivity> activities = dashboardService.getActivityFeed();

            assertThat(activities).isEmpty();
        }

        @Test
        @DisplayName("三路动态合并后按时间倒序，上限 10 条")
        void getActivityFeed_mergesAndSorts() {
            var now = LocalDateTime.now();
            when(adminUserPort.findRecentRegistrations(anyInt()))
                    .thenReturn(List.of(new RecentActivityRow("u-1", "张三", now.minusMinutes(30))));
            when(adminProductPort.findRecentPublished(anyInt()))
                    .thenReturn(List.of(new RecentActivityRow("p-1", "高等数学教材", now.minusMinutes(1))));
            when(adminOrderPort.findRecentCreated(anyInt()))
                    .thenReturn(List.of(new RecentActivityRow("o-1", "NO20260929001", now.minusMinutes(10))));

            List<DashboardActivity> activities = dashboardService.getActivityFeed();

            assertThat(activities).hasSize(3);
            // 时间倒序：商品(1 分钟前) → 订单(10 分钟前) → 用户(30 分钟前)
            assertThat(activities.get(0).text()).contains("高等数学教材");
            assertThat(activities.get(1).text()).contains("NO20260929001");
            assertThat(activities.get(2).text()).contains("张三");
            assertThat(activities).extracting(DashboardActivity::type).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("昵称为空时回落到用户 id，不出现 null 文案")
        void getActivityFeed_blankNickname_fallsBackToId() {
            when(adminUserPort.findRecentRegistrations(anyInt()))
                    .thenReturn(List.of(new RecentActivityRow("u-1", null, LocalDateTime.now())));
            when(adminProductPort.findRecentPublished(anyInt())).thenReturn(List.of());
            when(adminOrderPort.findRecentCreated(anyInt())).thenReturn(List.of());

            List<DashboardActivity> activities = dashboardService.getActivityFeed();

            assertThat(activities).hasSize(1);
            assertThat(activities.get(0).text()).contains("u-1").doesNotContain("null");
        }
    }
}
