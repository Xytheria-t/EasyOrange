package com.cartethyia.easyorange.product.adapter.outbound.persistence.report;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.report.ProductReportDO;
import org.springframework.context.annotation.Primary;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.report.ProductReportMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Primary
@Repository
public class ProductReportRepositoryImpl extends BaseRepository<ProductReportMapper, ProductReportDO> implements ProductReportRepository {

    public ProductReportRepositoryImpl(ProductReportMapper productReportMapper) {
        super(productReportMapper);
    }

    @Override
    public ProductReport findById(String id) {
        ProductReportDO reportDO = mapper.selectById(id);
        return convertToDomain(reportDO);
    }

    @Override
    public void save(ProductReport report) {
        ProductReportDO reportDO = convertToDO(report);
        mapper.insert(reportDO);
    }

    @Override
    public void update(ProductReport report) {
        ProductReportDO reportDO = convertToDO(report);
        mapper.updateById(reportDO);
    }

    @Override
    public boolean existsRecentReport(String productId, String reporterId) {
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