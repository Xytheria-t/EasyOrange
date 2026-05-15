package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;

import java.util.List;

public interface ProductReportRepository {

    ProductReport findById(Long id);

    List<ProductReport> findPendingReports(int pageNum, int pageSize);

    long countPendingReports();

    PageResult<ProductReport> findByStatus(Integer status, int pageNum, int pageSize);

    long countByStatus(Integer status);

    PageResult<ProductReport> findByReporterId(Long reporterId, int pageNum, int pageSize);

    void save(ProductReport report);

    void update(ProductReport report);

    boolean existsRecentReport(Long productId, Long reporterId);
}
