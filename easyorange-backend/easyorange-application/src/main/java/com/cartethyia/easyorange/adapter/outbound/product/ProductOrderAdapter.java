package com.cartethyia.easyorange.adapter.outbound.product;

import com.cartethyia.easyorange.order.domain.port.ProductOrderPort;

import com.cartethyia.easyorange.product.application.command.ProductCommandService;
import com.cartethyia.easyorange.product.domain.port.ProductSnapshotPort;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductOrderAdapter implements ProductOrderPort {

    private final ProductSnapshotPort productSnapshotPort;
    private final ProductCommandService productCommandService;

    @Override
    public Optional<ProductSnapshot> getSnapshot(String productId) {
        return productSnapshotPort.findSnapshot(ProductId.of(productId))
                .map(snapshot -> new ProductSnapshot(
                        snapshot.productId().value(),
                        snapshot.sellerId().value(),
                        snapshot.price().value(),
                        snapshot.status() == ProductStatus.ONLINE,
                        snapshot.stock().value(),
                        snapshot.location()
                ));
    }

    @Override
    public void decreaseStock(String productId, int quantity) {
        productCommandService.decrementStock(productId, quantity);
    }

    @Override
    public void restoreStock(String productId, int quantity) {
        productCommandService.restoreStock(productId, quantity);
    }

    @Override
    public void markAsSold(String productId) {
        productCommandService.markAsSold(productId);
    }
}
