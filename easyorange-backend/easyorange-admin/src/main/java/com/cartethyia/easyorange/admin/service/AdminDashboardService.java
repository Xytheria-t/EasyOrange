package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ActivityResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.DashboardStatsResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.PendingItemsResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.RecentProductResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.RecentUserResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.TopProductResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.TrendResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.UserActivityHeatmapResponse;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.application.port.query.ProductReportQueryRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserDO;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserMapper userMapper;
    private final ProductQueryRepository productQueryRepository;
    private final ProductReportQueryRepository productReportQueryRepository;
    private final OrderReadRepository orderReadRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int TREND_MONTHS = 6;
    private static final Map<String, String> PRODUCT_STATUS_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        for (ProductStatus s : ProductStatus.values()) {
            map.put(s.getCode(), s.getDesc());
        }
        PRODUCT_STATUS_MAP = Map.copyOf(map);
    }

    public DashboardStatsResponse getDashboardStats() {
        long totalUsers = ChainWrappers.lambdaQueryChain(userMapper)
            .eq(UserDO::getDelFlag, 0)
            .count();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayNewUsers = ChainWrappers.lambdaQueryChain(userMapper)
            .eq(UserDO::getDelFlag, 0)
            .ge(UserDO::getCreateTime, todayStart)
            .count();

        long totalProducts = productQueryRepository.countByStatus(null);
        long pendingProducts = productQueryRepository.countByStatus(ProductStatus.DRAFT.getCode());
        long totalOrders = orderReadRepository.countByStatus(null);
        long pendingReports = productReportQueryRepository.countPendingReports();

        return DashboardStatsResponse.builder()
            .totalUsers(totalUsers)
            .todayNewUsers(todayNewUsers)
            .totalProducts(totalProducts)
            .pendingProducts(pendingProducts)
            .totalOrders(totalOrders)
            .todayOrders(0L)
            .totalRevenue(0L)
            .pendingReports(pendingReports)
            .build();
    }

    public PendingItemsResponse getPendingItems() {
        long pendingReports = productReportQueryRepository.countPendingReports();
        long pendingOrders = orderReadRepository.countByStatus(Integer.valueOf(OrderStatus.PENDING_PAYMENT.getCode()));
        long pendingProducts = productQueryRepository.countByStatus(ProductStatus.DRAFT.getCode());

        List<PendingItemsResponse.PendingReportItem> recentReports = productReportQueryRepository
            .findPendingReports(1, 5)
            .stream()
            .map(report -> PendingItemsResponse.PendingReportItem.builder()
                .id(report.getId())
                .productId(report.getProductId())
                .reason(report.getReason())
                .createTime(report.getCreateTime() != null ? report.getCreateTime().toString() : null)
                .build())
            .toList();

        return PendingItemsResponse.builder()
            .pendingReports(pendingReports)
            .pendingOrders(pendingOrders)
            .pendingProducts(pendingProducts)
            .recentReports(recentReports)
            .build();
    }

    public List<RecentUserResponse> getRecentUsers(int limit) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        return ChainWrappers.lambdaQueryChain(userMapper)
            .eq(UserDO::getDelFlag, 0)
            .ge(UserDO::getCreateTime, todayStart)
            .orderByDesc(UserDO::getCreateTime)
            .last("LIMIT " + limit)
            .list()
            .stream()
            .map(this::toRecentUserResponse)
            .toList();
    }

    public List<RecentProductResponse> getRecentProducts(int limit) {
        return productQueryRepository.findProductsByIds(
            getRecentProductIds(limit)
        ).stream()
            .map(this::toRecentProductResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TrendResponse> getTrend() {
        LocalDate since = LocalDate.now().minusMonths(TREND_MONTHS);

        Map<String, Long> usersByMonth = countByMonth(
            "SELECT DATE_FORMAT(create_time, '%Y-%m') as month, COUNT(*) as cnt " +
            "FROM eo_user WHERE del_flag = 0 AND create_time >= ? " +
            "GROUP BY month ORDER BY month",
            since
        );
        Map<String, Long> productsByMonth = countByMonth(
            "SELECT DATE_FORMAT(create_time, '%Y-%m') as month, COUNT(*) as cnt " +
            "FROM eo_product WHERE del_flag = 0 AND create_time >= ? " +
            "GROUP BY month ORDER BY month",
            since
        );
        Map<String, Long> ordersByMonth = countByMonth(
            "SELECT DATE_FORMAT(create_time, '%Y-%m') as month, COUNT(*) as cnt " +
            "FROM eo_order WHERE del_flag = 0 AND create_time >= ? " +
            "GROUP BY month ORDER BY month",
            since
        );

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
                Stream.concat(getRecentOrderActivities(), getRecentReportActivities())
            )
            .sorted(Comparator.comparing(ActivityResponse::getTime).reversed())
            .limit(10)
            .toList();
    }

    private Stream<ActivityResponse> getRecentUserActivities() {
        return jdbcTemplate.queryForList(
            "SELECT id, nickname, create_time FROM eo_user WHERE del_flag = 0 ORDER BY create_time DESC LIMIT 5"
        ).stream().map(row -> {
            long id = ((Number) row.get("id")).longValue();
            String nickname = row.get("nickname") != null ? (String) row.get("nickname") : "用户" + id;
            return ActivityResponse.builder()
                .time(toLocalDateTime(row.get("create_time")).format(DATETIME_FORMAT))
                .text("新用户 " + nickname + " 完成注册")
                .type("user")
                .build();
        });
    }

    private Stream<ActivityResponse> getRecentProductActivities() {
        return jdbcTemplate.queryForList(
            "SELECT id, name, create_time FROM eo_product WHERE del_flag = 0 ORDER BY create_time DESC LIMIT 5"
        ).stream().map(row -> ActivityResponse.builder()
            .time(toLocalDateTime(row.get("create_time")).format(DATETIME_FORMAT))
            .text("商品「" + row.get("name") + "」发布上架")
            .type("product")
            .build());
    }

    private Stream<ActivityResponse> getRecentOrderActivities() {
        return jdbcTemplate.queryForList(
            "SELECT id, order_no, create_time FROM eo_order WHERE del_flag = 0 ORDER BY create_time DESC LIMIT 5"
        ).stream().map(row -> ActivityResponse.builder()
            .time(toLocalDateTime(row.get("create_time")).format(DATETIME_FORMAT))
            .text("订单 " + row.get("order_no") + " 创建成功")
            .type("order")
            .build());
    }

    private Stream<ActivityResponse> getRecentReportActivities() {
        return jdbcTemplate.queryForList(
            "SELECT id, reason, create_time FROM eo_product_report WHERE del_flag = 0 ORDER BY create_time DESC LIMIT 5"
        ).stream().map(row -> ActivityResponse.builder()
            .time(toLocalDateTime(row.get("create_time")).format(DATETIME_FORMAT))
            .text("收到1条新的举报: " + row.get("reason"))
            .type("report")
            .build());
    }

    @Transactional(readOnly = true)
    public List<UserActivityHeatmapResponse> getUserActivityHeatmap() {
        LocalDateTime since = LocalDate.now().minusDays(30).atStartOfDay();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT DAYOFWEEK(created_at) AS day_of_week, HOUR(created_at) AS hour, COUNT(*) AS cnt " +
            "FROM eo_audit_log WHERE created_at >= ? " +
            "GROUP BY DAYOFWEEK(oper_time), HOUR(oper_time) " +
            "ORDER BY day_of_week, hour",
            since
        );
        List<UserActivityHeatmapResponse> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new UserActivityHeatmapResponse(
                ((Number) row.get("day_of_week")).intValue(),
                ((Number) row.get("hour")).intValue(),
                ((Number) row.get("cnt")).longValue()
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> getTopProducts(int limit) {
        return jdbcTemplate.queryForList(
            "SELECT p.id, p.name, p.view_count, p.price, p.status, " +
            "(SELECT pi.image_url FROM eo_product_image pi WHERE pi.product_id = p.id AND pi.del_flag = 0 ORDER BY pi.is_main DESC, pi.sort_order ASC LIMIT 1) AS main_image " +
            "FROM eo_product p WHERE p.del_flag = 0 AND p.status = '1' " +
            "ORDER BY p.view_count DESC LIMIT " + limit
        ).stream()
            .map(row -> {
                String statusCode = row.get("status") != null ? row.get("status").toString() : null;
                return TopProductResponse.builder()
                    .productId(String.valueOf(row.get("id")))
                    .name((String) row.get("name"))
                    .viewCount(row.get("view_count") != null ? ((Number) row.get("view_count")).intValue() : 0)
                    .price(row.get("price") != null ? (java.math.BigDecimal) row.get("price") : java.math.BigDecimal.ZERO)
                    .mainImage((String) row.get("main_image"))
                    .status(statusCode)
                    .statusDesc(PRODUCT_STATUS_MAP.getOrDefault(statusCode, "未知"))
                    .build();
            })
            .toList();
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

    private List<String> getRecentProductIds(int limit) {
        return jdbcTemplate.queryForList(
            "SELECT id FROM eo_product WHERE del_flag = 0 ORDER BY create_time DESC LIMIT " + limit,
            String.class
        );
    }

    private RecentUserResponse toRecentUserResponse(UserDO entity) {
        return RecentUserResponse.builder()
            .userId(entity.getId())
            .username(entity.getUsername())
            .nickname(entity.getNickName())
            .avatar(entity.getAvatar())
            .email(entity.getEmail())
            .phone(entity.getPhone())
            .userType(entity.getUserType() != null ? entity.getUserType().getCode() : null)
            .userTypeDesc(entity.getUserType() != null ? entity.getUserType().getDescription() : null)
            .status(entity.getStatus() != null ? entity.getStatus().getCode() : null)
            .statusDesc(entity.getStatus() != null ? entity.getStatus().getDescription() : null)
            .createTime(entity.getCreateTime())
            .build();
    }

    private RecentProductResponse toRecentProductResponse(
        com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel model
    ) {
        return RecentProductResponse.builder()
            .productId(model.id())
            .sellerId(model.sellerId())
            .name(model.title())
            .price(model.price())
            .mainImage(model.mainImageUrl())
            .status(model.status())
            .statusDesc(model.statusDesc())
            .sellerId(model.sellerId())
            .sellerName(model.username())
            .categoryName(model.categoryName())
            .viewCount(model.views())
            .createTime(model.createTime())
            .build();
    }
}
