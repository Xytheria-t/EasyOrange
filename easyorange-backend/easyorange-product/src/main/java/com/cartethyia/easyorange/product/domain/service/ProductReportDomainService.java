package com.cartethyia.easyorange.product.domain.service;

import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.repository.ProductCachePort;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductStatusVO;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReportDomainService {

    private final ProductReportRepository productReportRepository;
    private final ProductRepository productRepository;
    private final ProductCachePort productCachePort;

    @Transactional(rollbackFor = Exception.class)
    public void reportProduct(Long productId, Long reporterId, String reason) {
        ProductReport report = ProductReport.create(productId, reporterId, reason);
        productReportRepository.save(report);
        log.info("商品举报已创建: productId={}, reporterId={}", productId, reporterId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void processReport(Long reportId, boolean approved) {
        ProductReport report = productReportRepository.findById(reportId);
        if (report == null) {
            throw new IllegalArgumentException("举报记录不存在: " + reportId);
        }

        if (approved) {
            report.approve(null);
            productRepository.updateStatus(
                    ProductId.of(report.getProductId()),
                    ProductStatusVO.of(ProductStatus.OFFLINE)
            );
            productCachePort.evictProductCache(report.getProductId());
            log.info("举报审核通过，商品已下线: productId={}", report.getProductId());
        } else {
            report.reject(null);
            log.info("举报审核驳回: reportId={}", reportId);
        }

        productReportRepository.update(report);
    }
}
