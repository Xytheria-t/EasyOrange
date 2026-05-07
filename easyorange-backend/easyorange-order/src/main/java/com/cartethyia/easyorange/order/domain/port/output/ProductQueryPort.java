package com.cartethyia.easyorange.order.domain.port.output;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductQueryPort {

    Optional<ProductDetail> getProductById(Long productId);

    List<ProductDetail> getProductsByIds(List<Long> productIds);

    record ProductDetail(
            Long id,
            String title,
            BigDecimal price,
            Integer status,
            List<String> images
    ) {}
}
