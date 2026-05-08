package com.cartethyia.easyorange.order.adapter.outbound.messaging;

import com.cartethyia.easyorange.order.domain.port.output.ProductInventoryPort;
import com.cartethyia.easyorange.product.application.command.ProductCommandService;
import com.cartethyia.easyorange.product.application.command.dto.DecrementStockCommand;
import com.cartethyia.easyorange.product.application.command.dto.MarkAsSoldCommand;
import com.cartethyia.easyorange.product.application.command.dto.RestoreStockCommand;
import com.cartethyia.easyorange.product.domain.port.ProductSnapshotPort;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductInventoryAdapter implements ProductInventoryPort {

    private final ProductSnapshotPort productSnapshotPort;
    private final ProductCommandService productCommandService;

    @Override
    public Optional<ProductSnapshot> getSnapshot(Long productId) {
        return productSnapshotPort.getOrderableSnapshot(ProductId.of(productId))
                .map(snapshot -> new ProductSnapshot(
                        snapshot.productId().value(),
                        snapshot.sellerId().value(),
                        snapshot.price().value(),
                        snapshot.status().isOnline(),
                        snapshot.stock().isAvailable(),
                        snapshot.location()
                ));
    }

    @Override
    public boolean decreaseStock(Long productId) {
        productCommandService.decrementStock(new DecrementStockCommand(productId));
        return true;
    }

    @Override
    public void restoreStock(Long productId) {
        productCommandService.restoreStock(new RestoreStockCommand(productId));
    }

    @Override
    public void markAsSold(Long productId) {
        productCommandService.markAsSold(new MarkAsSoldCommand(productId));
    }
}
