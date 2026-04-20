package com.cartethyia.easyorange.product.application.port.outbound;

import com.cartethyia.easyorange.product.domain.valueobject.Money;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductStatusVO;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;

import java.util.Optional;

public interface ProductSnapshotPort {

    Optional<ProductOrderSnapshot> getOrderableSnapshot(ProductId productId);

    record ProductOrderSnapshot(
            ProductId productId,
            SellerId sellerId,
            Money price,
            ProductStatusVO status,
            StockQuantity stock,
            Long version
    ) {}
}
