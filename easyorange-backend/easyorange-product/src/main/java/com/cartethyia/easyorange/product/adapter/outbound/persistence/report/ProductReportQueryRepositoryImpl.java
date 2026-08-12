package com.cartethyia.easyorange.product.adapter.outbound.persistence.report;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.port.query.ProductReportQueryRepository;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ProductReportQueryRepositoryImpl extends BaseRepository<ProductReportMapper, ProductReportDO>
        implements ProductReportQueryRepository {

    public ProductReportQueryRepositoryImpl(ProductReportMapper productReportMapper) {
        super(productReportMapper);
    }

    @Override
    public PageResult<ProductReport> findByStatus(String status, int pageNum, int pageSize) {
        Page<ProductReportDO> page = new Page<>(pageNum, pageSize);
        var wrapper = lambdaQuery();
        if (status != null) {
            wrapper.eq(ProductReportDO::getStatus, status);
        }
        wrapper.orderByDesc(ProductReportDO::getCreateTime);
        Page<ProductReportDO> resultPage = wrapper.page(page);
        List<ProductReport> reports =
                resultPage.getRecords().stream().map(this::convertToDomain).toList();

        return PageResult.of(reports, resultPage.getTotal(), pageNum, pageSize);
    }

    @Override
    public long countByStatus(String status) {
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
        List<ProductReport> reports =
                resultPage.getRecords().stream().map(this::convertToDomain).toList();

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
                do_.getReasonType());
    }
}
