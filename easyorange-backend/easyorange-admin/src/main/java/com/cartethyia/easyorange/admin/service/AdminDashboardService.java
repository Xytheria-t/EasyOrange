package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ActivityResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.DashboardStatsResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.TrendResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminDashboardPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderStats;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserStats;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final AdminUserPort adminUserPort;
    private final AdminDashboardPort adminDashboardPort;
    private final AdminOrderPort adminOrderPort;
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int TREND_MONTHS = 6;

    public DashboardStatsResponse getDashboardStats() {
        UserStats userStats = adminUserPort.getUserStats();
        AdminDashboardPort.ProductStats productStats = adminDashboardPort.getProductStats();
        OrderStats orderStats = adminOrderPort.getOrderStats();

        return DashboardStatsResponse.builder()
                .totalUsers(userStats.totalUsers())
                .todayNewUsers(userStats.todayNewUsers())
                .totalProducts(productStats.total())
                .pendingProducts(productStats.pending())
                .totalOrders(orderStats.totalOrders())
                .todayOrders(orderStats.todayOrders())
                .totalRevenue(orderStats.totalRevenue())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TrendResponse> getTrend() {
        LocalDate since = LocalDate.now().minusMonths(TREND_MONTHS);

        Map<String, Long> usersByMonth = countByMonth(
                "SELECT DATE_FORMAT(create_time, '%Y-%m') as month, COUNT(*) as cnt "
                        + "FROM eo_user WHERE del_flag = 0 AND create_time >= ? "
                        + "GROUP BY month ORDER BY month",
                since);
        Map<String, Long> productsByMonth = countByMonth(
                "SELECT DATE_FORMAT(create_time, '%Y-%m') as month, COUNT(*) as cnt "
                        + "FROM eo_product WHERE del_flag = 0 AND create_time >= ? "
                        + "GROUP BY month ORDER BY month",
                since);
        Map<String, Long> ordersByMonth = countByMonth(
                "SELECT DATE_FORMAT(create_time, '%Y-%m') as month, COUNT(*) as cnt "
                        + "FROM eo_order WHERE del_flag = 0 AND create_time >= ? "
                        + "GROUP BY month ORDER BY month",
                since);

        List<TrendResponse> result = new ArrayList<>();
        LocalDate cursor = since;
        while (!cursor.isAfter(LocalDate.now())) {
            String monthKey = cursor.format(MONTH_FORMAT);
            result.add(TrendResponse.builder()
                    .month(monthKey)
                    .users(usersByMonth.getOrDefault(monthKey, 0L))
                    .products(productsByMonth.getOrDefault(monthKey, 0L))
                    .orders(ordersByMonth.getOrDefault(monthKey, 0L))
                    .build());
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> getRecentActivity() {
        return Stream.concat(
                        Stream.concat(getRecentUserActivities(), getRecentProductActivities()),
                        getRecentOrderActivities())
                .sorted(Comparator.comparing(ActivityResponse::getTime).reversed())
                .limit(10)
                .toList();
    }

    private Stream<ActivityResponse> getRecentUserActivities() {
        return jdbcTemplate
                .queryForList(
                        "SELECT user_id, nick_name, create_time FROM eo_user WHERE del_flag = 0 ORDER BY create_time DESC LIMIT 5")
                .stream()
                .map(row -> {
                    String userId = String.valueOf(row.get("user_id"));
                    String nickname = row.get("nick_name") != null ? (String) row.get("nick_name") : "用户" + userId;
                    return ActivityResponse.builder()
                            .time(toLocalDateTime(row.get("create_time")).format(DATETIME_FORMAT))
                            .text("新用户 " + nickname + " 完成注册")
                            .type("user")
                            .build();
                });
    }

    private Stream<ActivityResponse> getRecentProductActivities() {
        return jdbcTemplate
                .queryForList(
                        "SELECT id, name, create_time FROM eo_product WHERE del_flag = 0 ORDER BY create_time DESC LIMIT 5")
                .stream()
                .map(row -> ActivityResponse.builder()
                        .time(toLocalDateTime(row.get("create_time")).format(DATETIME_FORMAT))
                        .text("商品「" + row.get("name") + "」发布上架")
                        .type("product")
                        .build());
    }

    private Stream<ActivityResponse> getRecentOrderActivities() {
        return jdbcTemplate
                .queryForList(
                        "SELECT id, order_no, create_time FROM eo_order WHERE del_flag = 0 ORDER BY create_time DESC LIMIT 5")
                .stream()
                .map(row -> ActivityResponse.builder()
                        .time(toLocalDateTime(row.get("create_time")).format(DATETIME_FORMAT))
                        .text("订单 " + row.get("order_no") + " 创建成功")
                        .type("order")
                        .build());
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        return LocalDateTime.now();
    }

    private Map<String, Long> countByMonth(String sql, LocalDate since) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, since.atStartOfDay());
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put((String) row.get("month"), ((Number) row.get("cnt")).longValue());
        }
        return result;
    }
}
