package com.cartethyia.easyorange.product.application.command.handler;

import com.cartethyia.easyorange.product.domain.service.ProductReportDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateProductReportHandler {

    private final ProductReportDomainService productReportDomainService;

    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long productId, Long reporterId, String reason) {
        productReportDomainService.reportProduct(productId, reporterId, reason);
    }
}
