package com.cartethyia.easyorange.order.domain.port.output;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductQueryPort extends OutboundPort {

    Optional<ProductDetail> getProductById(Long productId);

    List<ProductDetail> getProductsByIds(List<Long> productIds);

    record ProductDetail(
            Long id,
            String title,
            BigDecimal price,
            Integer status,
            List<String> images,
            String description,
            String conditionLevel
    ) {}
}
