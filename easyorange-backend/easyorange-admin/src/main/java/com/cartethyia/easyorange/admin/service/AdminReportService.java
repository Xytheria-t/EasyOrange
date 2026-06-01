package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.admin.dto.request.ReportHandleRequest;
import com.cartethyia.easyorange.admin.dto.request.BatchHandleRequest;
import com.cartethyia.easyorange.admin.dto.response.AdminReportResponse;
import com.cartethyia.easyorange.admin.dto.response.ReportStatsResponse;
import com.cartethyia.easyorange.admin.dto.response.ReportHandleHistoryResponse;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.entity.ReportHandleHistory;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.domain.event.ReportProcessedEvent;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ReportHandleHistoryRepository;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import com.cartethyia.easyorange.admin.util.BatchQueryUtil;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ReportHandleHistoryRepository reportHandleHistoryRepository;
    private final ProductMapper productMapper;
    private final BatchQueryUtil batchQueryUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResult<AdminReportResponse> listReports(Integer pageNum, Integer pageSize, Integer status) {
        int page = pageNum != null ? pageNum : 1;
        int size = pageSize != null ? pageSize : 20;

        PageResult<ProductReport> reportPage = productReportRepository.findByStatus(status, page, size);

        Map<Long, UserEntity> userMap = batchQueryUtil.batchGetUsers(reportPage.records().stream().map(ProductReport::getReporterId).distinct().toList());
        Map<Long, ProductDO> productMap = batchQueryUtil.batchGetProducts(reportPage.records().stream().map(ProductReport::getProductId).distinct().toList());

        List<AdminReportResponse> records = reportPage.records().stream()
            .map(r -> toAdminReportResponse(r, userMap, productMap))
            .collect(Collectors.toList());

        return PageResult.of(records, reportPage.total(), page, size);
    }

    @Transactional(readOnly = true)
    public AdminReportResponse getReportDetail(Long id) {
        ProductReport report = productReportRepository.findById(id);
        BizRequire.notNull(report, "举报记录不存在");

        Map<Long, UserEntity> userMap = batchQueryUtil.batchGetUsers(List.of(report.getReporterId()));
        Map<Long, ProductDO> productMap = batchQueryUtil.batchGetProducts(List.of(report.getProductId()));

        return toAdminReportResponse(report, userMap, productMap);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long id, ReportHandleRequest request) {
        Long operatorId = SecurityContextUtil.getCurrentUserIdOrThrow();
        String remark = request.getRemark() != null ? request.getRemark() : "";
        processSingleReport(id, request.getAction(), remark, operatorId);
    }

    @Transactional(readOnly = true)
    public List<ReportHandleHistoryResponse> getReportHistory(Long reportId) {
        List<ReportHandleHistory> histories = reportHandleHistoryRepository.findByReportId(reportId);

        Map<Long, UserEntity> operatorMap = batchQueryUtil.batchGetUsers(histories.stream().map(ReportHandleHistory::getOperatorId).distinct().toList());

        return histories.stream()
            .map(h -> toHistoryResponse(h, operatorMap))
            .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchHandleReports(BatchHandleRequest request) {
        BizRequire.notEmpty(request.getReportIds(), "举报ID列表不能为空");
        if (request.getReportIds().size() > 50) {
            throw BusinessException.of("批量处理数量不能超过50条");
        }

        Long operatorId = SecurityContextUtil.getCurrentUserIdOrThrow();
        String action = request.getAction();
        String remark = request.getRemark() != null ? request.getRemark() : "";

        for (Long reportId : request.getReportIds()) {
            try {
                processSingleReport(reportId, action, remark, operatorId);
            } catch (BusinessException e) {
                continue;
            }
        }
    }

    @Transactional(readOnly = true)
    public ReportStatsResponse getReportStats() {
        return ReportStatsResponse.builder()
            .totalReports(productReportRepository.countByStatus(null))
            .pendingReports(productReportRepository.countByStatus(ProductReportStatus.PENDING.getCode()))
            .processingReports(productReportRepository.countByStatus(ProductReportStatus.PROCESSING.getCode()))
            .resolvedReports(productReportRepository.countByStatus(ProductReportStatus.RESOLVED.getCode()))
            .dismissedReports(productReportRepository.countByStatus(ProductReportStatus.DISMISSED.getCode()))
            .build();
    }

    private void processSingleReport(Long reportId, String action, String remark, Long operatorId) {
        ProductReport report = productReportRepository.findById(reportId);
        BizRequire.notNull(report, "举报记录不存在");
        if (!report.isPending()) {
            throw BusinessException.of("该举报已被处理");
        }

        applyAction(report, action, remark);
        saveHandleHistory(reportId, operatorId, action, buildHistoryRemark(action, remark));
        productReportRepository.update(report);

        boolean approved = ProductReportStatus.RESOLVED.equals(report.getStatus());
        publishProcessedEvent(reportId, report, approved);
    }

    private void applyAction(ProductReport report, String action, String remark) {
        switch (action) {
            case "resolve" ->
                report.approve(remark.isEmpty() ? "举报已处理" : remark);
            case "dismiss" ->
                report.reject(remark.isEmpty() ? "举报已驳回" : remark);
            case "IGNORE" ->
                report.reject(remark.isEmpty() ? "管理员忽略" : remark);
            case "PRODUCT_OFFLINE" -> {
                handleProductOffline(report, remark);
                report.approve("下架商品: " + (remark.isEmpty() ? "" : remark));
            }
            case "WARN_SENDER" ->
                report.reject("警告举报人: " + remark);
            case "BAN_PRODUCT" -> {
                handleBanProduct(report, remark);
                report.approve("封禁商品: " + (remark.isEmpty() ? "" : remark));
            }
            default ->
                throw BusinessException.of("无效的处理动作");
        }
    }

    private String buildHistoryRemark(String action, String remark) {
        return switch (action) {
            case "resolve" -> remark.isEmpty() ? "举报已处理" : remark;
            case "dismiss" -> remark.isEmpty() ? "举报已驳回" : remark;
            case "IGNORE" -> remark.isEmpty() ? "管理员忽略" : remark;
            case "PRODUCT_OFFLINE" -> "下架商品: " + remark;
            case "WARN_SENDER" -> "警告举报人: " + remark;
            case "BAN_PRODUCT" -> "封禁商品: " + remark;
            default -> remark;
        };
    }

    private void publishProcessedEvent(Long reportId, ProductReport report, boolean approved) {
        ReportProcessedEvent event = new ReportProcessedEvent(
                reportId,
                report.getReporterId(),
                report.getProductId(),
                approved,
                report.getRemark(),
                LocalDateTime.now()
        );
        eventPublisher.publishEvent(event);
    }

    private void saveHandleHistory(Long reportId, Long operatorId, String action, String remark) {
        ReportHandleHistory history = ReportHandleHistory.create(reportId, operatorId, action, remark);
        reportHandleHistoryRepository.save(history);
    }

    private void handleProductOffline(ProductReport report, String remark) {
        ProductDO product = productMapper.selectById(report.getProductId());
        if (product != null && product.getDelFlag() == 0) {
            product.setStatus(2);
            productMapper.updateById(product);
        }
    }

    private void handleBanProduct(ProductReport report, String remark) {
        ProductDO product = productMapper.selectById(report.getProductId());
        if (product != null && product.getDelFlag() == 0) {
            product.setStatus(-1);
            productMapper.updateById(product);
        }
    }


    private AdminReportResponse toAdminReportResponse(
        ProductReport report,
        Map<Long, UserEntity> userMap,
        Map<Long, ProductDO> productMap
    ) {
        UserEntity reporter = userMap.get(report.getReporterId());
        ProductDO product = productMap.get(report.getProductId());

        String statusDesc = switch (report.statusCode()) {
            case 0 -> "待处理";
            case 1 -> "处理中";
            case 2 -> "已解决";
            case 3 -> "已驳回";
            default -> "未知";
        };

        String reasonTypeDesc = switch (report.getReasonType() != null ? report.getReasonType() : 0) {
            case 1 -> "虚假信息";
            case 2 -> "侵权投诉";
            case 3 -> "违规内容";
            case 4 -> "其他";
            default -> null;
        };

        LocalDateTime handleTime = report.isPending() ? null : report.getUpdateTime();

        return AdminReportResponse.builder()
            .reportId(report.getId())
            .productId(report.getProductId())
            .productName(product != null ? product.getName() : null)
            .productImage(null)
            .reporterId(report.getReporterId())
            .reporterName(reporter != null ? reporter.getNickName() : null)
            .reasonType(report.getReasonType())
            .reasonTypeDesc(reasonTypeDesc)
            .reason(report.getReason())
            .status(report.statusCode())
            .statusDesc(statusDesc)
            .handleResult(report.getRemark())
            .handleRemark(report.getRemark())
            .createTime(report.getCreateTime())
            .handleTime(handleTime)
            .build();
    }

    private ReportHandleHistoryResponse toHistoryResponse(
        ReportHandleHistory history,
        Map<Long, UserEntity> operatorMap
    ) {
        UserEntity operator = operatorMap.get(history.getOperatorId());

        String actionDesc = switch (history.getAction()) {
            case "resolve" -> "处理通过";
            case "dismiss" -> "驳回";
            case "IGNORE" -> "忽略";
            case "PRODUCT_OFFLINE" -> "下架商品";
            case "WARN_SENDER" -> "警告举报人";
            case "BAN_PRODUCT" -> "封禁商品";
            default -> history.getAction();
        };

        return ReportHandleHistoryResponse.builder()
            .id(history.getId())
            .reportId(history.getReportId())
            .operatorName(operator != null ? operator.getNickName() : null)
            .action(history.getAction())
            .actionDesc(actionDesc)
            .remark(history.getRemark())
            .createTime(history.getCreateTime())
            .build();
    }
}
