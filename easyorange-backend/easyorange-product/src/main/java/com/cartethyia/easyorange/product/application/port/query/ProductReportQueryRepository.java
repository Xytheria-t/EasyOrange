package com.cartethyia.easyorange.product.application.port.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;

public interface ProductReportQueryRepository {

    PageResult<ProductReport> findByStatus(String status, int pageNum, int pageSize);

    long countByStatus(String status);

    PageResult<ProductReport> findByReporterId(String reporterId, int pageNum, int pageSize);
}
