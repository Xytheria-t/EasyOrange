package com.cartethyia.easyorange.controller.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.controller.admin.dto.response.DashboardStatsVO;
import com.cartethyia.easyorange.controller.admin.dto.response.PendingItemsVO;
import com.cartethyia.easyorange.controller.admin.dto.response.RecentProductVO;
import com.cartethyia.easyorange.controller.admin.dto.response.RecentUserVO;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.output.OrderReadRepository;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserMapper userMapper;
    private final ProductQueryRepository productQueryRepository;
    private final ProductReportRepository productReportRepository;
    private final OrderReadRepository orderReadRepository;

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

    private List<Long> getRecentProductIds(int limit) {
        return List.of();
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
