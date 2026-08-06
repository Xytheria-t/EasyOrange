package com.cartethyia.easyorange.product.adapter.outbound.persistence.audit;

import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.product.domain.entity.ProductAuditLog;
import com.cartethyia.easyorange.product.domain.repository.ProductAuditLogRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class ProductAuditLogRepositoryImpl implements ProductAuditLogRepository {

    private final ProductAuditLogMapper productAuditLogMapper;
    private final ProductAuditLogDataMapper dataMapper;
    private final IdGenerator idGenerator;

    public ProductAuditLogRepositoryImpl(
            ProductAuditLogMapper productAuditLogMapper,
            @Qualifier("productAuditLogDataMapperImpl") ProductAuditLogDataMapper dataMapper,
            IdGenerator idGenerator) {
        this.productAuditLogMapper = productAuditLogMapper;
        this.dataMapper = dataMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public void save(ProductAuditLog auditLog) {
        var dataObject = dataMapper.toDataObject(auditLog);
        dataObject.setId(idGenerator.generateId());
        productAuditLogMapper.insert(dataObject);
    }

    @Override
    public List<ProductAuditLog> findByProductId(String productId) {
        return productAuditLogMapper.selectByProductId(productId).stream()
                .map(dataMapper::toDomainEntity)
                .toList();
    }
}
