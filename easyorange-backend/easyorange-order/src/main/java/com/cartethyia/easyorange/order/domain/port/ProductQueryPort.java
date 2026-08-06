package com.cartethyia.easyorange.order.domain.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductQueryPort {

    Optional<ProductDetail> getProductById(String productId);

    List<ProductDetail> getProductsByIds(List<String> productIds);

    record ProductDetail(
            String id,
            String title,
            BigDecimal price,
            String status,
            List<String> images,
            String description,
            String conditionLevel) {}
}
