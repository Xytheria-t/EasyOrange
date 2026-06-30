package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.domain.entity.ProductAuditLog;

import java.util.List;

public interface ProductAuditLogRepository {

    void save(ProductAuditLog auditLog);

    List<ProductAuditLog> findByProductId(String productId);
}
