package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.domain.entity.ProductReport;

public interface ProductReportRepository {

    ProductReport findById(String id);

    void save(ProductReport report);

    void update(ProductReport report);

    boolean existsRecentReport(String productId, String reporterId);
}
