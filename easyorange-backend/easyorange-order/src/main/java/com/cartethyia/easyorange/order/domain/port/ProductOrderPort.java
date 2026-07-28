package com.cartethyia.easyorange.order.domain.port;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductOrderPort {

    Optional<ProductSnapshot> getSnapshot(String productId);

    void decreaseStock(String productId, int quantity);

    void restoreStock(String productId, int quantity);

    default void restoreStock(String productId) {
        restoreStock(productId, 1);
    }

    void markAsSold(String productId);

    record ProductSnapshot(
            String productId,
            String sellerId,
            BigDecimal price,
            boolean isOnline,
            int stockQuantity,
            String location
    ) {
        public boolean hasStock() { return stockQuantity > 0; }
    }
}