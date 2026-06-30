package com.cartethyia.easyorange.order.domain.port;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductInventoryPort {

    Optional<ProductSnapshot> getSnapshot(String productId);

    boolean decreaseStock(String productId);

    void restoreStock(String productId);

    void markAsSold(String productId);

    record ProductSnapshot(
            String productId,
            String sellerId,
            BigDecimal price,
            boolean isOnline,
            boolean hasStock,
            String location
    ) {}
}