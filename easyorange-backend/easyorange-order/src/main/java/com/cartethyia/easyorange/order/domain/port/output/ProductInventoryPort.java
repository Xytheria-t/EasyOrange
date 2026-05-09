package com.cartethyia.easyorange.order.domain.port.output;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductInventoryPort extends OutboundPort {

    Optional<ProductSnapshot> getSnapshot(Long productId);

    boolean decreaseStock(Long productId);

    void restoreStock(Long productId);

    void markAsSold(Long productId);

    record ProductSnapshot(
            Long productId,
            Long sellerId,
            BigDecimal price,
            boolean isOnline,
            boolean hasStock,
            String location
    ) {}
}
