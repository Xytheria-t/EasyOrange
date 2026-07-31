package com.cartethyia.easyorange.product.adapter.outbound.persistence.audit;

import com.cartethyia.easyorange.product.domain.entity.ProductAuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductAuditLogDataMapper {

    ProductAuditLogDO toDataObject(ProductAuditLog auditLog);

    ProductAuditLog toDomainEntity(ProductAuditLogDO dataObject);
}
