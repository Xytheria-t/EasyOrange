package com.cartethyia.easyorange.product.adapter.outbound.persistence.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.repository.BaseRepository;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductReportDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductReportMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ProductReportRepositoryImpl extends BaseRepository<ProductReportMapper, ProductReportDO> implements ProductReportRepository {

    public ProductReportRepositoryImpl(ProductReportMapper productReportMapper) {
        super(productReportMapper);
    }

    @Override
    public ProductReport findById(Long id) {
        ProductReportDO reportDO = mapper.selectById(id);
        return convertToDomain(reportDO);
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
    public PageResult<ProductReport> findByReporterId(Long reporterId, int pageNum, int pageSize) {
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

    @Override
    public void save(ProductReport report) {
        ProductReportDO reportDO = convertToDO(report);
        mapper.insert(reportDO);
        report.assignId(reportDO.getId());
    }

    @Override
    public void update(ProductReport report) {
        ProductReportDO reportDO = convertToDO(report);
        updateById(reportDO);
    }

    @Override
    public boolean existsRecentReport(Long productId, Long reporterId) {
        return lambdaQuery()
                .eq(ProductReportDO::getProductId, productId)
                .eq(ProductReportDO::getReporterId, reporterId)
                .ge(ProductReportDO::getCreateTime, LocalDateTime.now().minusHours(24))
                .count() > 0;
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

    private ProductReportDO convertToDO(ProductReport report) {
        ProductReportDO.ProductReportDOBuilder builder = ProductReportDO.builder()
                .productId(report.getProductId())
                .reporterId(report.getReporterId())
                .reason(report.getReason())
                .status(report.statusCode())
                .handleResult(report.getRemark())
                .reasonType(report.getReasonType());

        if (report.getId() != null) {
            builder.id(report.getId());
        }

        return builder.build();
    }
}