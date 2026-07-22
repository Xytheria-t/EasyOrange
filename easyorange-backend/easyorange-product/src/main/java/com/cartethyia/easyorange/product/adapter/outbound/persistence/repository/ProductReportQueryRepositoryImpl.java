package com.cartethyia.easyorange.product.adapter.outbound.persistence.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.ProductReportDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductReportMapper;
import com.cartethyia.easyorange.product.application.port.query.ProductReportQueryRepository;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductReportQueryRepositoryImpl extends BaseRepository<ProductReportMapper, ProductReportDO> implements ProductReportQueryRepository {

    public ProductReportQueryRepositoryImpl(ProductReportMapper productReportMapper) {
        super(productReportMapper);
    }

    @Override
    public List<ProductReport> findPendingReports(int pageNum, int pageSize) {
        Page<ProductReportDO> page = new Page<>(pageNum, pageSize);
        Page<ProductReportDO> resultPage = lambdaQuery()
                .eq(ProductReportDO::getStatus, ProductReportStatus.PENDING.getCode())
                .orderByDesc(ProductReportDO::getCreateTime)
                .page(page);
        return resultPage.getRecords().stream()
                .map(this::convertToDomain)
                .toList();
    }

    @Override
    public long countPendingReports() {
        return lambdaQuery()
                .eq(ProductReportDO::getStatus, ProductReportStatus.PENDING.getCode())
                .count();
    }

    @Override
    public PageResult<ProductReport> findByStatus(Integer status, int pageNum, int pageSize) {
        Page<ProductReportDO> page = new Page<>(pageNum, pageSize);
        var wrapper = lambdaQuery();
        if (status != null) {
            wrapper.eq(ProductReportDO::getStatus, status);
        }
        wrapper.orderByDesc(ProductReportDO::getCreateTime);
        Page<ProductReportDO> resultPage = wrapper.page(page);
        List<ProductReport> reports = resultPage.getRecords().stream()
                .map(this::convertToDomain)
                .toList();

        return PageResult.of(reports, resultPage.getTotal(), pageNum, pageSize);
    }

    @Override
    public long countByStatus(Integer status) {
        var wrapper = lambdaQuery();
        if (status != null) {
            wrapper.eq(ProductReportDO::getStatus, status);
        }
        return wrapper.count();
    }

    @Override
    public PageResult<ProductReport> findByReporterId(String reporterId, int pageNum, int pageSize) {
        Page<ProductReportDO> page = new Page<>(pageNum, pageSize);
        Page<ProductReportDO> resultPage = lambdaQuery()
                .eq(ProductReportDO::getReporterId, reporterId)
                .orderByDesc(ProductReportDO::getCreateTime)
                .page(page);
        List<ProductReport> reports = resultPage.getRecords().stream()
                .map(this::convertToDomain)
                .toList();

        return PageResult.of(reports, resultPage.getTotal(), pageNum, pageSize);
    }

    private ProductReport convertToDomain(ProductReportDO do_) {
        if (do_ == null) {
            return null;
        }
        return ProductReport.reconstitute(
                do_.getId(),
                do_.getProductId(),
                do_.getReporterId(),
                do_.getReason(),
                ProductReportStatus.fromCode(do_.getStatus()),
                do_.getHandleResult(),
                do_.getCreateTime(),
                do_.getUpdateTime(),
                do_.getReasonType()
        );
    }
}
