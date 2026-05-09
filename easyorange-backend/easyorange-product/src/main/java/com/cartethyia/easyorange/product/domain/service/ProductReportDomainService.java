package com.cartethyia.easyorange.product.domain.service;

import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProductReportDomainService {

    private final ProductReportRepository productReportRepository;
    private final ProductRepository productRepository;
    private final ProductCachePort productCachePort;

    public void reportProduct(Long productId, Long reporterId, String reason) {
        ProductReport report = ProductReport.create(productId, reporterId, reason);
        productReportRepository.save(report);
    }

    public void processReport(Long reportId, boolean approved) {
        ProductReport report = productReportRepository.findById(reportId);
        if (report == null) {
            throw new IllegalArgumentException("举报记录不存在: " + reportId);
        }

        if (approved) {
            report.approve(null);
            productRepository.updateStatus(
                    ProductId.of(report.getProductId()),
                    ProductStatus.OFFLINE
            );
            productCachePort.evictProductCache(report.getProductId());
        } else {
            report.reject(null);
        }

        productReportRepository.update(report);
    }
}
