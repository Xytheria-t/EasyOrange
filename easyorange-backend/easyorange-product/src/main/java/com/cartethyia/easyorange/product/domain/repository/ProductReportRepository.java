package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;

import java.util.List;

public interface ProductReportRepository {

    ProductReport findById(Long id);

    List<ProductReport> findPendingReports(int pageNum, int pageSize);

    long countPendingReports();

    void save(ProductReport report);

    void update(ProductReport report);

    boolean existsRecentReport(Long productId, Long reporterId);
}
