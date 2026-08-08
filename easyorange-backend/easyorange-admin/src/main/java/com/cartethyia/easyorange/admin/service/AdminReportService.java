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
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ReportHistoryRecord;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ReportQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ReportRecord;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ReportStats;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
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

    private final AdminProductQueryPort adminProductQueryPort;
    private final AdminReportAssembler assembler;
    private final AdminUserQueryPort adminUserQueryPort;

    @Transactional(readOnly = true)
    public PageResult<AdminReportResponse> listReports(Integer pageNum, Integer pageSize, Integer status) {
        int page = pageNum != null ? pageNum : 1;
        int size = pageSize != null ? pageSize : 20;

        ReportQueryResult reportPage = adminProductQueryPort.queryReports(status, page, size);

        List<String> productIds = reportPage.records().stream()
                .map(ReportRecord::productId)
                .distinct()
                .toList();
        List<String> reporterIds = reportPage.records().stream()
                .map(ReportRecord::reporterId)
                .distinct()
                .toList();

        Map<String, AdminUserQueryPort.UserInfo> userMap = adminUserQueryPort.getUserInfos(reporterIds);
        Map<String, AdminProductQueryPort.ProductInfo> productMap = adminProductQueryPort.getProductInfos(productIds);
        Map<String, List<String>> imageMap = adminProductQueryPort.getProductImages(productIds);

        List<AdminReportResponse> records = reportPage.records().stream()
                .map(r -> assembler.toAdminReportResponse(
                        r,
                        userMap.get(r.reporterId()),
                        productMap.get(r.productId()),
                        firstImage(imageMap.get(r.productId()))))
                .toList();

        return PageResult.of(records, reportPage.total(), page, size);
    }

    @Transactional(readOnly = true)
    public AdminReportResponse getReportDetail(String id) {
        ReportRecord report = adminProductQueryPort.getReportDetail(id);
        BizRequire.notNull(report, AdminResultCode.REPORT_NOT_FOUND);

        AdminUserQueryPort.UserInfo reporter =
                adminUserQueryPort.getUserInfos(List.of(report.reporterId())).get(report.reporterId());
        AdminProductQueryPort.ProductInfo product = adminProductQueryPort
                .getProductInfos(List.of(report.productId()))
                .get(report.productId());
        String productImage = firstImage(adminProductQueryPort
                .getProductImages(List.of(report.productId()))
                .get(report.productId()));

        return assembler.toAdminReportResponse(report, reporter, product, productImage);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleReport(String id, ReportHandleRequest request) {
        String operatorId = SecurityContextUtil.getCurrentUserIdOrThrow();
        String remark = request.getRemark() != null ? request.getRemark() : "";
        adminProductQueryPort.handleReport(id, request.getAction(), remark, operatorId);
    }

    @Transactional(readOnly = true)
    public List<ReportHandleHistoryResponse> getReportHistory(String reportId) {
        List<ReportHistoryRecord> histories = adminProductQueryPort.getReportHistory(reportId);
        Map<String, AdminUserQueryPort.UserInfo> operatorMap = adminUserQueryPort.getUserInfos(histories.stream()
                .map(ReportHistoryRecord::operatorId)
                .distinct()
                .toList());

        return histories.stream()
                .map(h -> assembler.toHistoryResponse(h, operatorMap.get(h.operatorId())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public BatchHandleResultResponse batchHandleReports(BatchHandleRequest request) {
        BizRequire.requireTrue(!request.getReportIds().isEmpty(), AdminResultCode.REPORT_LIST_EMPTY);
        BizRequire.requireTrue(request.getReportIds().size() <= 50, AdminResultCode.REPORT_BATCH_LIMIT_EXCEEDED);

        String operatorId = SecurityContextUtil.getCurrentUserIdOrThrow();
        String remark = request.getRemark() != null ? request.getRemark() : "";
        ReportHandleAction.fromCode(request.getAction());

        List<String> errors = new ArrayList<>();
        int success = 0;
        for (String reportId : request.getReportIds()) {
            try {
                adminProductQueryPort.handleReport(reportId, request.getAction(), remark, operatorId);
                success++;
            } catch (BusinessException e) {
                errors.add("举报ID " + reportId + ": " + e.getMessage());
            }
        }
        return new BatchHandleResultResponse(request.getReportIds().size(), success, errors.size(), errors);
    }

    @Transactional(readOnly = true)
    public ReportStatsResponse getReportStats() {
        ReportStats stats = adminProductQueryPort.getReportStats();
        return ReportStatsResponse.builder()
                .totalReports(stats.total())
                .pendingReports(stats.pending())
                .processingReports(stats.processing())
                .resolvedReports(stats.resolved())
                .dismissedReports(stats.dismissed())
                .build();
    }

    private static String firstImage(List<String> images) {
        return images == null || images.isEmpty() ? null : images.getFirst();
    }
}
