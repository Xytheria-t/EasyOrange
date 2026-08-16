package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.metrics.BusinessMetricsService;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;
import com.cartethyia.easyorange.product.domain.enums.ReportReasonType;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.service.ProductReportDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductReportCommandHandler {

    private final ProductReportDomainService productReportDomainService;
    private final ProductReportRepository productReportRepository;
    private final BusinessMetricsService businessMetricsService;

    @Transactional(rollbackFor = Exception.class)
    public void handleReport(String productId, String reporterId, String reason, String reasonType) {
        if (!ReportReasonType.isValidCode(reasonType)) {
            throw BusinessException.of(ProductResultCode.INVALID_REPORT_TYPE);
        }
        if (productReportRepository.existsRecentReport(productId, reporterId)) {
            throw BusinessException.of(ProductResultCode.REPORT_DUPLICATE, "您已在24小时内举报过该商品，请耐心等待处理");
        }
        productReportDomainService.reportProduct(productId, reporterId, reason, reasonType);
        businessMetricsService.incrementReportFiled();
    }
}
