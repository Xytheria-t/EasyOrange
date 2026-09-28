package com.cartethyia.easyorange.admin.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ActivityResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.DashboardStatsResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.TrendResponse;
import com.cartethyia.easyorange.admin.domain.model.ActivityItem;
import com.cartethyia.easyorange.admin.domain.model.DashboardStats;
import com.cartethyia.easyorange.admin.domain.model.TrendPoint;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 仪表板出参组装 — 服务层只给 {@code domain/model} 里的视图，日期格式化与字段命名归 web 边界。
 * <p>
 * 时间格式化是展示口径（「05-01 14:30」怎么显示）而不是领域口径，故留在这一层。
 */
@Component
public class AdminDashboardAssembler {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public DashboardStatsResponse toStatsResponse(DashboardStats stats) {
        return DashboardStatsResponse.builder()
                .totalUsers(stats.totalUsers())
                .todayNewUsers(stats.todayNewUsers())
                .totalProducts(stats.totalProducts())
                .pendingProducts(stats.pendingProducts())
                .totalOrders(stats.totalOrders())
                .todayOrders(stats.todayOrders())
                .totalRevenue(stats.totalRevenue())
                .build();
    }

    public List<TrendResponse> toTrendResponses(List<TrendPoint> points) {
        return points.stream()
                .map(p -> TrendResponse.builder()
                        .month(p.month())
                        .users(p.users())
                        .products(p.products())
                        .orders(p.orders())
                        .build())
                .toList();
    }

    public List<ActivityResponse> toActivityResponses(List<ActivityItem> items) {
        return items.stream()
                .map(i -> ActivityResponse.builder()
                        .time(i.createdAt().format(DATETIME_FORMAT))
                        .text(i.text())
                        .type(i.type())
                        .build())
                .toList();
    }
}
