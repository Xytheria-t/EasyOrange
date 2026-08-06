package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminReportAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchHandleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ReportHandleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminReportResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchHandleResultResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ReportHandleHistoryResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ReportStatsResponse;
import com.cartethyia.easyorange.admin.domain.enums.AdminResultCode;
import com.cartethyia.easyorange.admin.domain.enums.ReportHandleAction;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.application.port.query.ProductReportQueryRepository;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.entity.ReportHandleHistory;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.domain.event.ReportProcessedEvent;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.repository.ReportHandleHistoryRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final ProductReportRepository productReportRepository;
    private final ProductReportQueryRepository productReportQueryRepository;
    private final ReportHandleHistoryRepository reportHandleHistoryRepository;
    private final ProductRepository productRepository;
    private final ProductCacheEvictionPort productCachePort;
    private final DomainEventPublisher domainEventPublisher;
    private final AdminReportAssembler assembler;
    private final AdminUserQueryPort adminUserQueryPort;
    private final AdminProductQueryPort adminProductQueryPort;

    @Transactional(readOnly = true)
    public PageResult<AdminReportResponse> listReports(Integer pageNum, Integer pageSize, Integer status) {
        int page = pageNum != null ? pageNum : 1;
        int size = pageSize != null ? pageSize : 20;

        PageResult<ProductReport> reportPage =
                productReportQueryRepository.findByStatus(status != null ? String.valueOf(status) : null, page, size);

        List<String> productIds = reportPage.records().stream()
                .map(ProductReport::getProductId)
                .distinct()
                .toList();
        List<String> reporterIds = reportPage.records().stream()
                .map(ProductReport::getReporterId)
                .distinct()
                .toList();

        Map<String, AdminUserQueryPort.UserInfo> userMap = adminUserQueryPort.getUserInfos(reporterIds);
        Map<String, AdminProductQueryPort.ProductInfo> productMap = adminProductQueryPort.getProductInfos(productIds);
        Map<String, List<String>> imageMap = adminProductQueryPort.getProductImages(productIds);

        List<AdminReportResponse> records = reportPage.records().stream()
                .map(r -> assembler.toAdminReportResponse(
                        r,
                        userMap.get(r.getReporterId()),
                        productMap.get(r.getProductId()),
                        firstImage(imageMap.get(r.getProductId()))))
                .toList();

        return PageResult.of(records, reportPage.total(), page, size);
    }

    @Transactional(readOnly = true)
    public AdminReportResponse getReportDetail(String id) {
        ProductReport report = productReportRepository.findById(id);
        BizRequire.notNull(report, AdminResultCode.REPORT_NOT_FOUND);

        AdminUserQueryPort.UserInfo reporter =
                adminUserQueryPort.getUserInfos(List.of(report.getReporterId())).get(report.getReporterId());
        AdminProductQueryPort.ProductInfo product = adminProductQueryPort
                .getProductInfos(List.of(report.getProductId()))
                .get(report.getProductId());
        String productImage = firstImage(adminProductQueryPort
                .getProductImages(List.of(report.getProductId()))
                .get(report.getProductId()));

        return assembler.toAdminReportResponse(report, reporter, product, productImage);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleReport(String id, ReportHandleRequest request) {
        String operatorId = SecurityContextUtil.getCurrentUserIdOrThrow();
        String remark = request.getRemark() != null ? request.getRemark() : "";
        processSingleReport(id, ReportHandleAction.fromCode(request.getAction()), remark, operatorId);
    }

    @Transactional(readOnly = true)
    public List<ReportHandleHistoryResponse> getReportHistory(String reportId) {
        List<ReportHandleHistory> histories = reportHandleHistoryRepository.findByReportId(reportId);
        Map<String, AdminUserQueryPort.UserInfo> operatorMap = adminUserQueryPort.getUserInfos(histories.stream()
                .map(ReportHandleHistory::getOperatorId)
                .distinct()
                .toList());

        return histories.stream()
                .map(h -> assembler.toHistoryResponse(h, operatorMap.get(h.getOperatorId())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public BatchHandleResultResponse batchHandleReports(BatchHandleRequest request) {
        BizRequire.requireTrue(!request.getReportIds().isEmpty(), AdminResultCode.REPORT_LIST_EMPTY);
        BizRequire.requireTrue(request.getReportIds().size() <= 50, AdminResultCode.REPORT_BATCH_LIMIT_EXCEEDED);

        String operatorId = SecurityContextUtil.getCurrentUserIdOrThrow();
        String remark = request.getRemark() != null ? request.getRemark() : "";
        ReportHandleAction action = ReportHandleAction.fromCode(request.getAction());

        List<String> errors = new ArrayList<>();
        int success = 0;
        for (String reportId : request.getReportIds()) {
            try {
                processSingleReport(reportId, action, remark, operatorId);
                success++;
            } catch (BusinessException e) {
                errors.add("举报ID " + reportId + ": " + e.getMessage());
            }
        }
        return new BatchHandleResultResponse(request.getReportIds().size(), success, errors.size(), errors);
    }

    @Transactional(readOnly = true)
    public ReportStatsResponse getReportStats() {
        return ReportStatsResponse.builder()
                .totalReports(productReportQueryRepository.countByStatus(null))
                .pendingReports(productReportQueryRepository.countByStatus(ProductReportStatus.PENDING.getCode()))
                .processingReports(productReportQueryRepository.countByStatus(ProductReportStatus.PROCESSING.getCode()))
                .resolvedReports(productReportQueryRepository.countByStatus(ProductReportStatus.RESOLVED.getCode()))
                .dismissedReports(productReportQueryRepository.countByStatus(ProductReportStatus.DISMISSED.getCode()))
                .build();
    }

    private void processSingleReport(String reportId, ReportHandleAction action, String remark, String operatorId) {
        ProductReport report = productReportRepository.findById(reportId);
        BizRequire.notNull(report, AdminResultCode.REPORT_NOT_FOUND);
        if (!report.isPending()) {
            throw BusinessException.of(AdminResultCode.REPORT_ALREADY_HANDLED);
        }

        String result = action.describe(remark);
        ProductReport updated =
                switch (action) {
                    case PRODUCT_OFFLINE -> {
                        handleProductOffline(report);
                        yield report.approve(result);
                    }
                    case BAN_PRODUCT -> {
                        handleBanProduct(report, remark);
                        yield report.approve(result);
                    }
                    case RESOLVE -> report.approve(result);
                    case DISMISS, IGNORE, WARN_SENDER -> report.reject(result);
                };

        reportHandleHistoryRepository.save(ReportHandleHistory.create(reportId, operatorId, action.getCode(), result));
        productReportRepository.update(updated);
        publishProcessedEvent(reportId, updated);
    }

    private void publishProcessedEvent(String reportId, ProductReport report) {
        domainEventPublisher.publish(new ReportProcessedEvent(
                reportId,
                report.getReporterId(),
                report.getProductId(),
                ProductReportStatus.RESOLVED.equals(report.getStatus()),
                report.getRemark(),
                LocalDateTime.now()));
    }

    private void handleProductOffline(ProductReport report) {
        var product = productRepository
                .findById(ProductId.of(report.getProductId()))
                .orElseThrow(() -> BusinessException.of(AdminResultCode.REPORT_PRODUCT_NOT_FOUND));
        productRepository.save(product.takeOffline().aggregate());
        productCachePort.evictProductCache(report.getProductId());
    }

    private void handleBanProduct(ProductReport report, String remark) {
        var product = productRepository
                .findById(ProductId.of(report.getProductId()))
                .orElseThrow(() -> BusinessException.of(AdminResultCode.REPORT_PRODUCT_NOT_FOUND));
        productRepository.save(product.reject("举报封禁: " + remark).aggregate());
        productCachePort.evictProductCache(report.getProductId());
    }

    private static String firstImage(List<String> images) {
        return images == null || images.isEmpty() ? null : images.getFirst();
    }
}
