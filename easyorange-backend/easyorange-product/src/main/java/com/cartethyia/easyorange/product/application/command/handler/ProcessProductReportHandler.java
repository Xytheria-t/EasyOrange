package com.cartethyia.easyorange.product.application.command.handler;

import com.cartethyia.easyorange.product.domain.service.ProductReportDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessProductReportHandler {

    private final ProductReportDomainService productReportDomainService;

    @Transactional(rollbackFor = Exception.class)
    public void handleApprove(Long reportId) {
        productReportDomainService.processReport(reportId, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleReject(Long reportId) {
        productReportDomainService.processReport(reportId, false);
    }
}
