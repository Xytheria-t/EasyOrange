package com.cartethyia.easyorange.product.domain.port;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import java.util.List;
import java.util.Optional;

public interface ProductSnapshotPort {

    Optional<ProductOrderSnapshot> findSnapshot(ProductId productId);

    default List<ProductOrderSnapshot> findSnapshots(List<ProductId> productIds) {
        return productIds.stream()
                .map(this::findSnapshot)
                .flatMap(Optional::stream)
                .toList();
    }

    record ProductOrderSnapshot(
            ProductId productId,
            SellerId sellerId,
            Money price,
            ProductStatus status,
            StockQuantity stock,
            String location) {}
}
