package com.cartethyia.easyorange.product.application.command.handler;

import com.cartethyia.easyorange.product.domain.service.ProductReportDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessProductReportHandler {

    private final ProductReportDomainService productReportDomainService;

    @Transactional(rollbackFor = Exception.class)
    public void handleApprove(Long reportId) {
        productReportDomainService.processReport(reportId, true);
        log.info("举报已批准: reportId={}", reportId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleReject(Long reportId) {
        productReportDomainService.processReport(reportId, false);
        log.info("举报已驳回: reportId={}", reportId);
    }
}