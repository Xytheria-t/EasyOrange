package com.cartethyia.easyorange.product.domain.port;

import com.cartethyia.easyorange.product.domain.valueobject.Money;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;

import java.util.Optional;

public interface ProductSnapshotPort {

    Optional<ProductOrderSnapshot> getOrderableSnapshot(ProductId productId);

    record ProductOrderSnapshot(
            ProductId productId,
            SellerId sellerId,
            Money price,
            ProductStatus status,
            StockQuantity stock,
            Long version,
            String location
    ) { }
}
