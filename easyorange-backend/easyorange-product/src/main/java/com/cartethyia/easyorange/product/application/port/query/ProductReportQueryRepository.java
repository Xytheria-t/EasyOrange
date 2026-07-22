package com.cartethyia.easyorange.product.application.port.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;

import java.util.List;

public interface ProductReportQueryRepository {

    List<ProductReport> findPendingReports(int pageNum, int pageSize);

    long countPendingReports();

    PageResult<ProductReport> findByStatus(Integer status, int pageNum, int pageSize);

    long countByStatus(Integer status);

    PageResult<ProductReport> findByReporterId(String reporterId, int pageNum, int pageSize);
}
