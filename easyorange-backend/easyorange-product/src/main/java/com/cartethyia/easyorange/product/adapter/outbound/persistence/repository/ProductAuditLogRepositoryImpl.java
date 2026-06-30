package com.cartethyia.easyorange.product.adapter.outbound.persistence.repository;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductAuditLogDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductAuditLogMapper;
import com.cartethyia.easyorange.product.domain.entity.ProductAuditLog;
import com.cartethyia.easyorange.product.domain.repository.ProductAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductAuditLogRepositoryImpl implements ProductAuditLogRepository {

    private final ProductAuditLogMapper productAuditLogMapper;

    @Override
    public void save(ProductAuditLog auditLog) {
        ProductAuditLogDO auditLogDO = toDataObject(auditLog);
        productAuditLogMapper.insert(auditLogDO);
    }

    @Override
    public List<ProductAuditLog> findByProductId(String productId) {
        List<ProductAuditLogDO> logs = productAuditLogMapper.selectByProductId(productId);
        return logs.stream().map(this::toDomainEntity).toList();
    }

    private ProductAuditLogDO toDataObject(ProductAuditLog domain) {
        return ProductAuditLogDO.builder()
                .productId(domain.getProductId())
                .operatorId(domain.getOperatorId())
                .operatorName(domain.getOperatorName())
                .action(domain.getAction())
                .reason(domain.getReason())
                .auditDimensions(domain.getAuditDimensions())
                .beforeStatus(domain.getBeforeStatus())
                .afterStatus(domain.getAfterStatus())
                .remark(domain.getRemark())
                .build();
    }

    private ProductAuditLog toDomainEntity(ProductAuditLogDO dataObject) {
        return ProductAuditLog.builder()
                .productId(dataObject.getProductId())
                .operatorId(dataObject.getOperatorId())
                .operatorName(dataObject.getOperatorName())
                .action(dataObject.getAction())
                .reason(dataObject.getReason())
                .auditDimensions(dataObject.getAuditDimensions())
                .beforeStatus(dataObject.getBeforeStatus())
                .afterStatus(dataObject.getAfterStatus())
                .remark(dataObject.getRemark())
                .build();
    }
}
