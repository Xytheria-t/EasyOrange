package com.cartethyia.easyorange.admin.application.service;

import com.cartethyia.easyorange.admin.domain.model.ActivityItem;
import com.cartethyia.easyorange.admin.domain.model.DashboardStats;
import com.cartethyia.easyorange.admin.domain.model.RecentActivity;
import com.cartethyia.easyorange.admin.domain.model.TrendPoint;
import com.cartethyia.easyorange.admin.domain.port.AdminDashboardPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderStats;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserStats;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端仪表板编排 — 三个模块的只读统计拼成一块屏。
 * <p>
 * <b>取舍</b>：本类不持任何 {@code JdbcTemplate}，跨模块统计一律经 {@code Admin*Port}。
 * 早前这里是 6 段内联 SQL 直查 {@code eo_user} / {@code eo_product} / {@code eo_order} ——
 * 表结构与软删口径在 SQL 里各存了一份，他模块改列名或加过滤条件时这块屏静默出错，
 * 而 admin 模块的隔离性正靠「不碰他模块的表」这一点成立。
 * <p>
 * <b>边界</b>：趋势按自然月补零（库里没数据的月份也要出现，否则前端折线断点），
 * 「最近动态」合并三路后统一倒序截断 —— 三条流水线的排序口径在服务层统一，不下沉到各端口。
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardAppService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    /** 趋势起点：自 6 个月前起逐自然月补零，含当月共 7 个点（前端按数组长度渲染「近 N 个月」，不是 6）。 */
    private static final int TREND_MONTHS_BACK = 6;

    private static final int RECENT_LIMIT = 10;
    private static final int RECENT_PER_SOURCE = 5;

    private final AdminUserPort adminUserPort;
    private final AdminDashboardPort adminDashboardPort;
    private final AdminOrderPort adminOrderPort;
    private final AdminProductPort adminProductPort;

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        UserStats userStats = adminUserPort.getUserStats();
        AdminDashboardPort.ProductStats productStats = adminDashboardPort.getProductStats();
        OrderStats orderStats = adminOrderPort.getOrderStats();

        return new DashboardStats(
                userStats.totalUsers(),
                userStats.todayNewUsers(),
                productStats.total(),
                productStats.pending(),
                orderStats.totalOrders(),
                orderStats.todayOrders(),
                orderStats.totalRevenue());
    }

    @Transactional(readOnly = true)
    public List<TrendPoint> getTrend() {
        LocalDate since = LocalDate.now().minusMonths(TREND_MONTHS_BACK);

        Map<String, Long> usersByMonth = adminUserPort.getCreateTrend(since);
        Map<String, Long> productsByMonth = adminProductPort.getCreateTrend(since);
        Map<String, Long> ordersByMonth = adminOrderPort.getCreateTrend(since);

        List<TrendPoint> result = new ArrayList<>();
        LocalDate cursor = since;
        while (!cursor.isAfter(LocalDate.now())) {
            String monthKey = cursor.format(MONTH_FORMAT);
            result.add(new TrendPoint(
                    monthKey,
                    usersByMonth.getOrDefault(monthKey, 0L),
                    productsByMonth.getOrDefault(monthKey, 0L),
                    ordersByMonth.getOrDefault(monthKey, 0L)));
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ActivityItem> getRecentActivity() {
        return Stream.concat(Stream.concat(userActivities(), productActivities()), orderActivities())
                .sorted(Comparator.comparing(ActivityItem::createdAt).reversed())
                .limit(RECENT_LIMIT)
                .toList();
    }

    private Stream<ActivityItem> userActivities() {
        return adminUserPort.findRecentRegistrations(RECENT_PER_SOURCE).stream()
                .map(a -> activity(a, "user", "新用户 " + nicknameOf(a) + " 完成注册"));
    }

    private Stream<ActivityItem> productActivities() {
        return adminProductPort.findRecentPublished(RECENT_PER_SOURCE).stream()
                .map(a -> activity(a, "product", "商品「" + a.display() + "」发布上架"));
    }

    private Stream<ActivityItem> orderActivities() {
        return adminOrderPort.findRecentCreated(RECENT_PER_SOURCE).stream()
                .map(a -> activity(a, "order", "订单 " + a.display() + " 创建成功"));
    }

    private static ActivityItem activity(RecentActivity activity, String type, String text) {
        return new ActivityItem(type, text, activity.createdAt());
    }

    /** 昵称可空，展示层不允许出现「null」。 */
    private static String nicknameOf(RecentActivity activity) {
        String display = activity.display();
        return display == null || display.isBlank() ? activity.id() : display;
    }
}
