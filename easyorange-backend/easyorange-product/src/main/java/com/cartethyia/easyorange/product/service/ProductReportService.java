package com.cartethyia.easyorange.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.entity.ProductReport;

public interface ProductReportService extends IService<ProductReport> {

    void reportProduct(Long productId, Long reporterId, String reason);

    PageResult<ProductReport> getPendingReports(int pageNum, int pageSize);

    void processReport(Long reportId, boolean approve);
}
