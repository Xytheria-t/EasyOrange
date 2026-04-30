package com.cartethyia.easyorange.product.application.command.handler;

import com.cartethyia.easyorange.product.domain.service.ProductReportDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateProductReportHandler {

    private final ProductReportDomainService productReportDomainService;

    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long productId, Long reporterId, String reason) {
        productReportDomainService.reportProduct(productId, reporterId, reason);
        log.info("商品举报成功: productId={}, reporterId={}", productId, reporterId);
    }
}