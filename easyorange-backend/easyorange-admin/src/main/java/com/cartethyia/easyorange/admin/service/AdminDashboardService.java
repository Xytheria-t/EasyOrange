package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.admin.dto.response.ActivityVO;
import com.cartethyia.easyorange.admin.dto.response.DashboardStatsVO;
import com.cartethyia.easyorange.admin.dto.response.PendingItemsVO;
import com.cartethyia.easyorange.admin.dto.response.RecentProductVO;
import com.cartethyia.easyorange.admin.dto.response.RecentUserVO;
import com.cartethyia.easyorange.admin.dto.response.TrendVO;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.output.OrderReadRepository;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserMapper userMapper;
    private final ProductQueryRepository productQueryRepository;
    private final ProductReportRepository productReportRepository;
    private final OrderReadRepository orderReadRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int TREND_MONTHS = 6;

    public DashboardStatsVO getDashboardStats() {
        long totalUsers = userMapper.selectCount(
            new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getDelFlag, 0)
        );

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayNewUsers = userMapper.selectCount(
            new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getDelFlag, 0)
                .ge(UserEntity::getCreateTime, todayStart)
        );

        long totalProducts = productQueryRepository.countByStatus(null);
        long pendingProducts = productQueryRepository.countByStatus(ProductStatus.DRAFT.getCode());
        long totalOrders = orderReadRepository.countByStatus(null);
        long todayOrders = countTodayOrders();
        long pendingReports = productReportRepository.countPendingReports();

        return DashboardStatsVO.builder()
            .totalUsers(totalUsers)
            .todayNewUsers(todayNewUsers)
            .totalProducts(totalProducts)
            .pendingProducts(pendingProducts)
            .totalOrders(totalOrders)
            .todayOrders(todayOrders)
            .totalRevenue(0L)
            .pendingReports(pendingReports)
            .build();
    }

    public PendingItemsVO getPendingItems() {
        long pendingReports = productReportRepository.countPendingReports();
        long pendingOrders = orderReadRepository.countByStatus(OrderStatus.PENDING_PAYMENT.getCode());
        long pendingProducts = productQueryRepository.countByStatus(ProductStatus.DRAFT.getCode());

        List<PendingItemsVO.PendingReportItem> recentReports = productReportRepository
            .findPendingReports(1, 5)
            .stream()
            .map(report -> PendingItemsVO.PendingReportItem.builder()
                .id(report.getId())
                .productId(report.getProductId())
                .reason(report.getReason())
                .createTime(report.getCreateTime() != null ? report.getCreateTime().toString() : null)
                .build())
            .collect(Collectors.toList());

        return PendingItemsVO.builder()
            .pendingReports(pendingReports)
            .pendingOrders(pendingOrders)
            .pendingProducts(pendingProducts)
            .recentReports(recentReports)
            .build();
    }

    public List<RecentUserVO> getRecentUsers(int limit) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        return userMapper.selectList(
            new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getDelFlag, 0)
                .ge(UserEntity::getCreateTime, todayStart)
                .orderByDesc(UserEntity::getCreateTime)
                .last("LIMIT " + limit)
        ).stream()
            .map(this::toRecentUserVO)
            .collect(Collectors.toList());
    }

    public List<RecentProductVO> getRecentProducts(int limit) {
        return productQueryRepository.findProductsByIds(
            getRecentProductIds(limit)
        ).stream()
            .map(this::toRecentProductVO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrendVO> getTrend() {
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

        List<TrendVO> result = new ArrayList<>();
        LocalDate cursor = since;
        while (!cursor.isAfter(LocalDate.now())) {
            String monthKey = cursor.format(MONTH_FORMAT);
            result.add(TrendVO.builder()
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
    public List<ActivityVO> getRecentActivity() {
        List<ActivityVO> activities = new ArrayList<>();

        // Recent user registrations
        List<Map<String, Object>> userRows = jdbcTemplate.queryForList(
            "SELECT id, nickname, create_time FROM eo_user WHERE del_flag = 0 ORDER BY create_time DESC LIMIT 5"
        );
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Map<String, Object> row : userRows) {
            long id = ((Number) row.get("id")).longValue();
            String nickname = row.get("nickname") != null ? (String) row.get("nickname") : "用户" + id;
            activities.add(ActivityVO.builder()
                .time(toLocalDateTime(row.get("create_time")).format(dtf))
                .text("新用户 " + nickname + " 完成注册")
                .type("user")
                .build());
        }

        // Recent products
        List<Map<String, Object>> productRows = jdbcTemplate.queryForList(
            "SELECT id, name, create_time FROM eo_product WHERE del_flag = 0 ORDER BY create_time DESC LIMIT 5"
        );
        for (Map<String, Object> row : productRows) {
            activities.add(ActivityVO.builder()
                .time(toLocalDateTime(row.get("create_time")).format(dtf))
                .text("商品「" + row.get("name") + "」发布上架")
                .type("product")
                .build());
        }

        // Recent orders
        List<Map<String, Object>> orderRows = jdbcTemplate.queryForList(
            "SELECT id, order_no, create_time FROM eo_order WHERE del_flag = 0 ORDER BY create_time DESC LIMIT 5"
        );
        for (Map<String, Object> row : orderRows) {
            activities.add(ActivityVO.builder()
                .time(toLocalDateTime(row.get("create_time")).format(dtf))
                .text("订单 " + row.get("order_no") + " 创建成功")
                .type("order")
                .build());
        }

        // Recent reports
        List<Map<String, Object>> reportRows = jdbcTemplate.queryForList(
            "SELECT id, reason, create_time FROM eo_product_report ORDER BY create_time DESC LIMIT 5"
        );
        for (Map<String, Object> row : reportRows) {
            activities.add(ActivityVO.builder()
                .time(toLocalDateTime(row.get("create_time")).format(dtf))
                .text("收到1条新的举报: " + row.get("reason"))
                .type("report")
                .build());
        }

        activities.sort(Comparator.comparing(ActivityVO::getTime).reversed());
        return activities.stream().limit(10).collect(Collectors.toList());
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

    private List<Long> getRecentProductIds(int limit) {
        return jdbcTemplate.queryForList(
            "SELECT id FROM eo_product WHERE del_flag = 0 ORDER BY create_time DESC LIMIT ?",
            Long.class, limit
        );
    }

    private long countTodayOrders() {
        return 0L;
    }

    private RecentUserVO toRecentUserVO(UserEntity entity) {
        return RecentUserVO.builder()
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

    private RecentProductVO toRecentProductVO(
        com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel model
    ) {
        return RecentProductVO.builder()
            .productId(model.id())
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
