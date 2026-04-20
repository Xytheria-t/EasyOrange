package com.cartethyia.easyorange.product.adapter.outbound.product;

import com.cartethyia.easyorange.product.application.port.outbound.ProductSnapshotPort;
import com.cartethyia.easyorange.product.application.port.outbound.ProductSnapshotPort.ProductOrderSnapshot;
import com.cartethyia.easyorange.product.domain.aggregate.ProductAggregate;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductSnapshotPortImpl implements ProductSnapshotPort {

    private final ProductRepository productRepository;

    @Override
    public Optional<ProductOrderSnapshot> getOrderableSnapshot(ProductId productId) {
        return productRepository.findById(productId)
                .map(this::toSnapshot);
    }

    private ProductOrderSnapshot toSnapshot(ProductAggregate aggregate) {
        return new ProductOrderSnapshot(
                aggregate.getId(),
                aggregate.getSellerId(),
                aggregate.getPrice(),
                aggregate.getStatus(),
                aggregate.getStock(),
                aggregate.getVersion() != null ? aggregate.getVersion().value().longValue() : null
        );
    }
}
