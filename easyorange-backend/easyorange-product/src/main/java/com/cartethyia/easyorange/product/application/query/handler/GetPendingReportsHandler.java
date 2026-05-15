package com.cartethyia.easyorange.product.application.query.handler;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetPendingReportsHandler {

    private final ProductReportRepository productReportRepository;

    @Transactional(readOnly = true)
    public PageResult<ProductReportResponse> handle(int pageNum, int pageSize) {
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
