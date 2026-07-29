package com.cartethyia.easyorange.product.domain.service;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.event.ProductTakeOfflineEvent;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class ProductReportDomainService {

    private final ProductReportRepository productReportRepository;
    private final ProductRepository productRepository;
    private final ProductCacheEvictionPort productCachePort;

    /**
     * Creates and saves a new product report.
     *
     * @param productId  the ID of the reported product
     * @param reporterId the ID of the user submitting the report
     * @param reason     the description of the report
     * @param reasonType the category code of the report
     */
    public void reportProduct(String productId, String reporterId, String reason, String reasonType) {
        ProductReport report = ProductReport.create(productId, reporterId, reason, reasonType);
        productReportRepository.save(report);
    }

    /**
     * Processes a product report by approving or rejecting it.
     * <p>
     * When approved, the reported product is taken offline via the aggregate
     * (preserving domain invariants and producing a domain event).
     * When rejected, the report is marked as rejected without affecting the product.
     *
     * @param reportId the ID of the report to process
     * @param approved {@code true} to approve the report and take the product offline,
     *                 {@code false} to reject the report
     * @return the domain event if the product was taken offline, {@link Optional#empty()} otherwise
     * @throws ReportNotFoundException if no report exists with the given ID
     */
    public Optional<ProductTakeOfflineEvent> processReport(String reportId, boolean approved) {
        ProductReport report = productReportRepository.findById(reportId);
        if (report == null) {
            throw new ReportNotFoundException("举报记录不存在: " + reportId);
        }

        ProductReport updated;
        Optional<ProductTakeOfflineEvent> event = Optional.empty();
        if (approved) {
            updated = report.approve(null);
            event = Optional.of(takeProductOffline(report.getProductId()));
        } else {
            updated = report.reject(null);
        }

        productReportRepository.update(updated);
        return event;
    }

    private ProductTakeOfflineEvent takeProductOffline(String productId) {
        Product product = productRepository.findById(ProductId.of(productId))
                .orElseThrow(() -> new ReportNotFoundException("商品不存在: " + productId));
        var t = product.takeOffline();
        productRepository.update(t.aggregate());
        productCachePort.evictProductCache(productId);
        return t.event();
    }

    public static class ReportNotFoundException extends BaseBusinessException {
        public ReportNotFoundException(String message) {
            super(message);
        }
        @Override
        protected String defaultCode() {
            return ProductResultCode.REPORT_NOT_FOUND.getCode();
        }
    }
}
