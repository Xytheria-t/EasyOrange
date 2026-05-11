package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.admin.dto.request.ReportHandleRequest;
import com.cartethyia.easyorange.admin.dto.response.AdminReportVO;
import com.cartethyia.easyorange.admin.dto.response.ReportStatsVO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final ProductReportRepository productReportRepository;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    public PageResult<AdminReportVO> listReports(Integer pageNum, Integer pageSize, Integer status) {
        int page = pageNum != null ? pageNum : 1;
        int size = pageSize != null ? pageSize : 20;

        List<ProductReport> reports;
        if (status != null) {
            if (status == 0) {
                reports = productReportRepository.findPendingReports(page, size);
            } else {
                reports = List.of();
            }
        } else {
            reports = productReportRepository.findPendingReports(1, page * size);
        }

        Map<Long, UserEntity> userMap = batchGetUsers(reports);
        Map<Long, ProductDO> productMap = batchGetProducts(reports);

        List<AdminReportVO> records = reports.stream()
            .map(r -> toAdminReportVO(r, userMap, productMap))
            .collect(Collectors.toList());

        return PageResult.of(records, records.size(), page, size);
    }

    public AdminReportVO getReportDetail(Long id) {
        ProductReport report = productReportRepository.findById(id);
        if (report == null) {
            throw BusinessException.of("举报记录不存在");
        }

        Map<Long, UserEntity> userMap = batchGetUsers(List.of(report));
        Map<Long, ProductDO> productMap = batchGetProducts(List.of(report));

        return toAdminReportVO(report, userMap, productMap);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long id, ReportHandleRequest request) {
        ProductReport report = productReportRepository.findById(id);
        if (report == null) {
            throw BusinessException.of("举报记录不存在");
        }
        if (!report.isPending()) {
            throw BusinessException.of("该举报已被处理");
        }

        switch (request.getAction()) {
            case "IGNORE" -> report.reject(request.getRemark() != null ? request.getRemark() : "管理员忽略");
            case "PRODUCT_OFFLINE" -> handleProductOffline(report, request);
            case "WARN_SENDER" -> handleWarnSender(report, request);
            case "BAN_PRODUCT" -> handleBanProduct(report, request);
            default -> throw BusinessException.of("无效的处理动作");
        }

        productReportRepository.update(report);
    }

    public ReportStatsVO getReportStats() {
        long pendingReports = productReportRepository.countPendingReports();
        return ReportStatsVO.builder()
            .totalReports(pendingReports)
            .pendingReports(pendingReports)
            .resolvedReports(0L)
            .dismissedReports(0L)
            .build();
    }

    private void handleProductOffline(ProductReport report, ReportHandleRequest request) {
        ProductDO product = productMapper.selectById(report.getProductId());
        if (product != null && product.getDelFlag() == 0) {
            product.setStatus(2);
            productMapper.updateById(product);
        }
        report.approve("下架商品: " + (request.getRemark() != null ? request.getRemark() : ""));
    }

    private void handleWarnSender(ProductReport report, ReportHandleRequest request) {
        report.reject("警告举报人: " + (request.getRemark() != null ? request.getRemark() : ""));
    }

    private void handleBanProduct(ProductReport report, ReportHandleRequest request) {
        ProductDO product = productMapper.selectById(report.getProductId());
        if (product != null && product.getDelFlag() == 0) {
            product.setStatus(-1);
            productMapper.updateById(product);
        }
        report.approve("封禁商品: " + (request.getRemark() != null ? request.getRemark() : ""));
    }

    private Map<Long, UserEntity> batchGetUsers(List<ProductReport> reports) {
        List<Long> userIds = reports.stream()
            .map(ProductReport::getReporterId)
            .distinct()
            .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<UserEntity> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(UserEntity::getId, u -> u, (a, b) -> a));
    }

    private Map<Long, ProductDO> batchGetProducts(List<ProductReport> reports) {
        List<Long> productIds = reports.stream()
            .map(ProductReport::getProductId)
            .distinct()
            .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductDO> products = productMapper.selectBatchIds(productIds);
        return products.stream().collect(Collectors.toMap(ProductDO::getId, p -> p, (a, b) -> a));
    }

    private AdminReportVO toAdminReportVO(
        ProductReport report,
        Map<Long, UserEntity> userMap,
        Map<Long, ProductDO> productMap
    ) {
        UserEntity reporter = userMap.get(report.getReporterId());
        ProductDO product = productMap.get(report.getProductId());

        String statusDesc = switch (report.statusCode()) {
            case 0 -> "待处理";
            case 1 -> "已处理";
            case 2 -> "已忽略";
            default -> "未知";
        };

        return AdminReportVO.builder()
            .reportId(report.getId())
            .productId(report.getProductId())
            .productName(product != null ? product.getName() : null)
            .productImage(null)
            .reporterId(report.getReporterId())
            .reporterName(reporter != null ? reporter.getNickName() : null)
            .reason(report.getReason())
            .status(report.statusCode())
            .statusDesc(statusDesc)
            .handleResult(report.getRemark())
            .createTime(report.getCreateTime())
            .handleTime(LocalDateTime.now())
            .build();
    }
}
