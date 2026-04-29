package com.cartethyia.easyorange.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.entity.ProductReport;
import com.cartethyia.easyorange.product.entity.Product;
import com.cartethyia.easyorange.product.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.enums.ProductStatus;
import com.cartethyia.easyorange.product.mapper.ProductReportMapper;
import com.cartethyia.easyorange.product.mapper.ProductMapper;
import com.cartethyia.easyorange.product.service.ProductReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductReportServiceImpl extends ServiceImpl<ProductReportMapper, ProductReport> implements ProductReportService {

    private final ProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportProduct(Long productId, Long reporterId, String reason) {
        boolean alreadyReported = lambdaQuery()
                .eq(ProductReport::getProductId, productId)
                .eq(ProductReport::getReporterId, reporterId)
                .ge(ProductReport::getCreateTime, LocalDateTime.now().minusHours(24))
                .exists();

        if (alreadyReported) {
            throw BusinessException.of("24小时内已举报过此商品");
        }

        ProductReport report = ProductReport.builder()
                .productId(productId)
                .reporterId(reporterId)
                .reason(reason)
                .status(ProductReportStatus.PENDING.getCode())
                .build();

        save(report);
    }

    @Override
    public PageResult<ProductReport> getPendingReports(int pageNum, int pageSize) {
        Page<ProductReport> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProductReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReport::getStatus, ProductReportStatus.PENDING.getCode())
                .orderByDesc(ProductReport::getCreateTime);

        Page<ProductReport> resultPage = page(page, wrapper);
        return PageResult.of(resultPage.getRecords(), resultPage.getTotal(),
                (int) resultPage.getCurrent(), (int) resultPage.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReport(Long reportId, boolean approve) {
        ProductReport report = getById(reportId);
        if (report == null) {
            throw BusinessException.of("举报记录不存在");
        }

        Product product = productMapper.selectById(report.getProductId());
        if (product == null) {
            throw BusinessException.of("商品不存在");
        }

        if (approve) {
            report.setStatus(ProductReportStatus.RESOLVED.getCode());
            product.setStatus(ProductStatus.OFFLINE.getCode());
            productMapper.updateById(product);
        } else {
            report.setStatus(ProductReportStatus.DISMISSED.getCode());
        }

        updateById(report);
    }
}
