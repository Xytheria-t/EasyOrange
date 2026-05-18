package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.dto.response.ActivityResponse;
import com.cartethyia.easyorange.admin.dto.response.DashboardStatsResponse;
import com.cartethyia.easyorange.admin.dto.response.PendingItemsResponse;
import com.cartethyia.easyorange.admin.dto.response.RecentUserResponse;
import com.cartethyia.easyorange.admin.dto.response.TopProductResponse;
import com.cartethyia.easyorange.admin.dto.response.TrendResponse;
import com.cartethyia.easyorange.admin.dto.response.UserActivityHeatmapResponse;
import com.cartethyia.easyorange.order.domain.port.output.OrderReadRepository;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDashboardService 单元测试")
class AdminDashboardServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProductQueryRepository productQueryRepository;

    @Mock
    private ProductReportRepository productReportRepository;

    @Mock
    private OrderReadRepository orderReadRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AdminDashboardService dashboardService;

    private UserEntity createTestUser() {
        return UserEntity.builder()
                .id(1L)
                .username("testuser")
                .nickName("测试用户")
                .userType(UserType.fromCode("01"))
                .status(UserStatus.NORMAL)
                .createTime(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getDashboardStats")
    class GetDashboardStatsTests {

        @Test
        @DisplayName("获取仪表盘统计数据")
        void getDashboardStats_returnsStats() {
            when(userMapper.selectCount(any())).thenReturn(100L, 5L);
            when(productQueryRepository.countByStatus(null)).thenReturn(200L);
            when(productQueryRepository.countByStatus(0)).thenReturn(10L);
            when(orderReadRepository.countByStatus(null)).thenReturn(300L);
            when(productReportRepository.countPendingReports()).thenReturn(8L);

            DashboardStatsResponse stats = dashboardService.getDashboardStats();

            assertThat(stats.getTotalUsers()).isEqualTo(100);
            assertThat(stats.getTodayNewUsers()).isEqualTo(5);
            assertThat(stats.getTotalProducts()).isEqualTo(200);
            assertThat(stats.getPendingProducts()).isEqualTo(10);
            assertThat(stats.getTotalOrders()).isEqualTo(300);
            assertThat(stats.getPendingReports()).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("getPendingItems")
    class GetPendingItemsTests {

        @Test
        @DisplayName("获取待处理事项")
        void getPendingItems_returnsItems() {
            when(productReportRepository.countPendingReports()).thenReturn(3L);
            when(orderReadRepository.countByStatus(anyInt())).thenReturn(5L);
            when(productQueryRepository.countByStatus(anyInt())).thenReturn(7L);
            when(productReportRepository.findPendingReports(anyInt(), anyInt()))
                    .thenReturn(List.of(
                            ProductReport.reconstitute(1L, 100L, 1L,
                                    "虚假信息", null, null,
                                    LocalDateTime.now(), LocalDateTime.now(), 1)
                    ));

            PendingItemsResponse items = dashboardService.getPendingItems();

            assertThat(items.getPendingReports()).isEqualTo(3);
            assertThat(items.getPendingOrders()).isEqualTo(5);
            assertThat(items.getPendingProducts()).isEqualTo(7);
            assertThat(items.getRecentReports()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getRecentUsers")
    class GetRecentUsersTests {

        @Test
        @DisplayName("获取最近注册用户")
        void getRecentUsers_returnsUsers() {
            when(userMapper.selectList(any())).thenReturn(List.of(createTestUser()));

            List<RecentUserResponse> users = dashboardService.getRecentUsers(5);

            assertThat(users).hasSize(1);
            assertThat(users.get(0).getUsername()).isEqualTo("testuser");
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
            userRow.put("id", 1L);
            userRow.put("nickname", "张三");
            userRow.put("create_time", Timestamp.valueOf(LocalDateTime.now()));

            Map<String, Object> productRow = new HashMap<>();
            productRow.put("id", 1L);
            productRow.put("name", "高等数学教材");
            productRow.put("create_time", Timestamp.valueOf(LocalDateTime.now()));

            when(jdbcTemplate.queryForList(anyString()))
                .thenReturn(List.of(userRow))
                .thenReturn(List.of(productRow))
                .thenReturn(List.of())
                .thenReturn(List.of());

            List<ActivityResponse> activities = dashboardService.getRecentActivity();

            assertThat(activities).hasSize(2);
            assertThat(activities.get(0).getText()).contains("张三");
            assertThat(activities.get(1).getText()).contains("高等数学教材");
        }
    }

    @Nested
    @DisplayName("getUserActivityHeatmap")
    class GetUserActivityHeatmapTests {

        @Test
        @DisplayName("获取用户活跃热力图数据")
        void getUserActivityHeatmap_returnsHeatmap() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("day_of_week", 1);
            row1.put("hour", 9);
            row1.put("cnt", 42L);

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("day_of_week", 1);
            row2.put("hour", 10);
            row2.put("cnt", 38L);

            when(jdbcTemplate.queryForList(anyString(), any(LocalDateTime.class)))
                .thenReturn(List.of(row1, row2));

            List<UserActivityHeatmapResponse> result = dashboardService.getUserActivityHeatmap();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).dayOfWeek()).isEqualTo(1);
            assertThat(result.get(0).hour()).isEqualTo(9);
            assertThat(result.get(0).count()).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("getTopProducts")
    class GetTopProductsTests {

        @Test
        @DisplayName("获取 Top 浏览量商品")
        void getTopProducts_returnsProducts() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", 1L);
            row.put("name", "高等数学教材");
            row.put("view_count", 1024);
            row.put("price", new java.math.BigDecimal("59.00"));
            row.put("main_image", "http://example.com/img.jpg");
            row.put("status", 1);

            when(jdbcTemplate.queryForList(anyString(), anyInt())).thenReturn(List.of(row));

            List<TopProductResponse> result = dashboardService.getTopProducts(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("高等数学教材");
            assertThat(result.get(0).getViewCount()).isEqualTo(1024);
            assertThat(result.get(0).getPrice()).isEqualByComparingTo(new java.math.BigDecimal("59.00"));
        }
    }
}
