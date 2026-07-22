package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.enums.ReportReasonType;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportDetailResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductReportQueryService {

    private final ProductReportRepository productReportRepository;
    private final ProductQueryRepository productQueryRepository;

    @Transactional(readOnly = true)
    public PageResult<ProductReportResponse> getMyReports(String reporterId, int pageNum, int pageSize) {
        PageResult<ProductReport> reportPage = productReportRepository.findByReporterId(reporterId, pageNum, pageSize);

        List<ProductReportResponse> voList = reportPage.records().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResult.of(voList, reportPage.total(), pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public ProductReportDetailResponse getReportDetail(String reportId, String currentUserId) {
        ProductReport report = productReportRepository.findById(reportId);
        if (report == null) {
            throw BusinessException.of("举报记录不存在");
        }
        if (!report.getReporterId().equals(currentUserId)) {
            throw BusinessException.of("无权查看此举报记录");
        }

        String statusDesc = switch (report.statusCode()) {
            case 0 -> "待处理";
            case 1 -> "处理中";
            case 2 -> "已解决";
            case 3 -> "已驳回";
            default -> "未知";
        };

        ReportReasonType reasonType = ReportReasonType.fromCode(report.getReasonType());

        String productName = null;
        ProductReadModel product = productQueryRepository.findProductById(report.getProductId());
        if (product != null) {
            productName = product.title();
        }

        return new ProductReportDetailResponse(
                report.getId(),
                report.getProductId(),
                productName,
                report.getReasonType(),
                reasonType != null ? reasonType.getDesc() : null,
                report.getReason(),
                report.statusCode(),
                statusDesc,
                report.getRemark(),
                report.getCreateTime(),
                report.getUpdateTime()
        );
    }

    @Transactional(readOnly = true)
    public PageResult<ProductReportResponse> getPendingReports(int pageNum, int pageSize) {
        List<ProductReport> reports = productReportRepository.findPendingReports(pageNum, pageSize);
        long total = productReportRepository.countPendingReports();

        List<ProductReportResponse> voList = reports.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResult.of(voList, total, pageNum, pageSize);
    }

    private ProductReportResponse toResponse(ProductReport report) {
        if (report == null) {
            return null;
        }
        return new ProductReportResponse(
                report.getId(),
                report.getProductId(),
                report.getReporterId(),
                report.getReason(),
                report.getReasonType(),
                report.statusCode()
        );
    }
}
