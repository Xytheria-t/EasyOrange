package com.cartethyia.easyorange.adapter.outbound.product;

import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import com.cartethyia.easyorange.product.application.command.ProductCommandHandler;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.port.ProductSnapshotPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class ProductInventoryAdapter implements ProductInventoryPort {

    private final ProductSnapshotPort productSnapshotPort;
    private final ProductCommandHandler productCommandHandler;

    @Override
    public List<ProductSnapshot> getSnapshots(List<String> productIds) {
        return productSnapshotPort
                .findSnapshots(productIds.stream().map(ProductId::of).toList())
                .stream()
                .map(ProductInventoryAdapter::toSnapshot)
                .toList();
    }

    private static ProductSnapshot toSnapshot(ProductSnapshotPort.ProductSnapshot snapshot) {
        return new ProductSnapshot(
                snapshot.productId().value(),
                snapshot.sellerId().value(),
                snapshot.price().value(),
                snapshot.status() == ProductStatus.ONLINE,
                snapshot.stock().value());
    }

    @Override
    public void decreaseStock(String productId, int quantity) {
        productCommandHandler.decrementStock(productId, quantity);
    }

    @Override
    public void restoreStock(String productId, int quantity) {
        productCommandHandler.restoreStock(productId, quantity);
    }

    @Override
    public void markAsSold(String productId) {
        productCommandHandler.markAsSold(productId);
    }
}
