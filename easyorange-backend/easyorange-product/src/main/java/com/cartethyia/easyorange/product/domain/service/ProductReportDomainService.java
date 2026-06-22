package com.cartethyia.easyorange.product.domain.service;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
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
    private final ProductCachePort<?> productCachePort;

    /**
     * Creates and saves a new product report.
     *
     * @param productId  the ID of the reported product
     * @param reporterId the ID of the user submitting the report
     * @param reason     the description of the report
     * @param reasonType the category code of the report
     */
    public void reportProduct(Long productId, Long reporterId, String reason, Integer reasonType) {
        ProductReport report = ProductReport.create(productId, reporterId, reason, reasonType);
        productReportRepository.save(report);
    }

    /**
     * Processes a product report by approving or rejecting it.
     * <p>
     * When approved, the reported product is taken offline and its cache is evicted.
     * When rejected, the report is marked as rejected without affecting the product.
     *
     * @param reportId the ID of the report to process
     * @param approved {@code true} to approve the report and take the product offline,
     *                 {@code false} to reject the report
     * @throws ReportNotFoundException if no report exists with the given ID
     */
    public void processReport(Long reportId, boolean approved) {
        ProductReport report = productReportRepository.findById(reportId);
        if (report == null) {
            throw new ReportNotFoundException("举报记录不存在: " + reportId);
        }

        ProductReport updated;
        if (approved) {
            updated = report.approve(null);
            productRepository.updateStatus(
                    ProductId.of(report.getProductId()),
                    ProductStatus.OFFLINE
            );
            productCachePort.evictProductCache(report.getProductId());
        } else {
            updated = report.reject(null);
        }

        productReportRepository.update(updated);
    }

    public static class ReportNotFoundException extends BaseBusinessException {
        public ReportNotFoundException(String message) {
            super(message);
        }
    }
}
