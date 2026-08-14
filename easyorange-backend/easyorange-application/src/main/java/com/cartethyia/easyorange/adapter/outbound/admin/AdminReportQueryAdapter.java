package com.cartethyia.easyorange.adapter.outbound.admin;

import com.cartethyia.easyorange.admin.domain.enums.AdminResultCode;
import com.cartethyia.easyorange.admin.domain.enums.ReportHandleAction;
import com.cartethyia.easyorange.admin.domain.port.AdminReportQueryPort;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.idgen.UuidV7;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.product.application.port.query.ProductReportQueryRepository;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.entity.ReportHandleHistory;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.domain.enums.ReportReasonType;
import com.cartethyia.easyorange.product.domain.event.ReportProcessedEvent;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.repository.ReportHandleHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Admin 举报查询/处理适配器
 * <p>
 * 实现 {@link AdminReportQueryPort}，通过 ProductReport Repository 访问举报数据并转换为 Admin 模块需要的格式。
 */
@Primary
@Component
@RequiredArgsConstructor
public class AdminReportQueryAdapter implements AdminReportQueryPort {

    private final ProductReportQueryRepository productReportQueryRepository;
    private final ProductReportRepository productReportRepository;
    private final ReportHandleHistoryRepository reportHandleHistoryRepository;
    private final ProductRepository productRepository;
    private final ProductCacheEvictionPort productCacheEvictionPort;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public ReportQueryResult queryReports(Integer status, Integer pageNum, Integer pageSize) {
        int page = pageNum != null ? pageNum : 1;
        int size = pageSize != null ? pageSize : 20;

        PageResult<ProductReport> reportPage =
                productReportQueryRepository.findByStatus(status != null ? String.valueOf(status) : null, page, size);

        List<ReportRecord> records =
                reportPage.records().stream().map(this::toReportRecord).toList();
        return new ReportQueryResult(records, reportPage.total(), page, size);
    }

    @Override
    public ReportRecord getReportDetail(String reportId) {
        ProductReport report = productReportRepository.findById(reportId);
        return report != null ? toReportRecord(report) : null;
    }

    @Override
    public List<ReportHistoryRecord> getReportHistory(String reportId) {
        return reportHandleHistoryRepository.findByReportId(reportId).stream()
                .map(this::toReportHistoryRecord)
                .toList();
    }

    @Override
    public ReportStats getReportStats() {
        return new ReportStats(
                productReportQueryRepository.countByStatus(null),
                productReportQueryRepository.countByStatus(ProductReportStatus.PENDING.getCode()),
                productReportQueryRepository.countByStatus(ProductReportStatus.PROCESSING.getCode()),
                productReportQueryRepository.countByStatus(ProductReportStatus.RESOLVED.getCode()),
                productReportQueryRepository.countByStatus(ProductReportStatus.DISMISSED.getCode()));
    }

    @Override
    public void handleReport(String reportId, String actionCode, String remark, String operatorId) {
        ProductReport report = productReportRepository.findById(reportId);
        BizRequire.notNull(report, AdminResultCode.REPORT_NOT_FOUND);
        if (!report.isPending()) {
            throw BusinessException.of(AdminResultCode.REPORT_ALREADY_HANDLED);
        }

        ReportHandleAction action = ReportHandleAction.fromCode(actionCode);
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
                UuidV7.generateId(),
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
        productCacheEvictionPort.evictProductCache(report.getProductId());
    }

    private void handleBanProduct(ProductReport report, String remark) {
        var product = productRepository
                .findById(ProductId.of(report.getProductId()))
                .orElseThrow(() -> BusinessException.of(AdminResultCode.REPORT_PRODUCT_NOT_FOUND));
        productRepository.save(product.reject("举报封禁: " + remark).aggregate());
        productCacheEvictionPort.evictProductCache(report.getProductId());
    }

    private ReportRecord toReportRecord(ProductReport report) {
        return new ReportRecord(
                report.getId(),
                report.getProductId(),
                report.getReporterId(),
                report.getReasonType(),
                reasonTypeDesc(report.getReasonType()),
                report.getReason(),
                report.statusCode(),
                statusDesc(report.statusCode()),
                report.getRemark(),
                report.getCreateTime(),
                report.getUpdateTime(),
                report.isPending());
    }

    private ReportHistoryRecord toReportHistoryRecord(ReportHandleHistory history) {
        return new ReportHistoryRecord(
                history.getId(),
                history.getReportId(),
                history.getOperatorId(),
                history.getAction(),
                history.getRemark(),
                history.getCreateTime());
    }

    private String statusDesc(String code) {
        if (code == null) {
            return null;
        }
        try {
            return ProductReportStatus.fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }

    private String reasonTypeDesc(String code) {
        if (code == null) {
            return null;
        }
        try {
            return ReportReasonType.fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
