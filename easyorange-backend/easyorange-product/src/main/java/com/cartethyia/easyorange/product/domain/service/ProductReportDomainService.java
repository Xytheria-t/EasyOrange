package com.cartethyia.easyorange.product.domain.service;

import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;
import com.cartethyia.easyorange.product.domain.event.ProductTakeOfflineEvent;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductReportDomainService {

    private final ProductReportRepository productReportRepository;
    private final ProductRepository productRepository;
    private final ProductCacheEvictionPort productCachePort;

    /**
     * 创建并保存一条商品举报。
     *
     * @param productId  被举报的商品 ID
     * @param reporterId 提交举报的用户 ID
     * @param reason     举报描述
     * @param reasonType 举报类型编码
     */
    public void reportProduct(String productId, String reporterId, String reason, String reasonType) {
        ProductReport report = ProductReport.create(productId, reporterId, reason, reasonType);
        productReportRepository.save(report);
    }

    /**
     * 处理举报：通过或驳回。
     * <p>
     * 通过时经由聚合根将商品下架（保持领域不变量并产生领域事件）；
     * 驳回时仅将举报标记为已驳回，不影响商品。
     *
     * @param reportId 举报 ID
     * @param approved {@code true} 通过举报并将商品下架，{@code false} 驳回举报
     * @return 商品下架领域事件；未下架时返回 {@link Optional#empty()}
     * @throws ReportNotFoundException 举报记录不存在
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
        Product product = productRepository
                .findById(ProductId.of(productId))
                .orElseThrow(() -> new ReportNotFoundException("商品不存在: " + productId));
        var t = product.takeOffline();
        productRepository.save(t.aggregate());
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
