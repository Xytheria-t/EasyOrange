package com.cartethyia.easyorange.product.adapter.outbound.persistence;

import com.cartethyia.easyorange.product.domain.port.ProductSnapshotPort;
import com.cartethyia.easyorange.product.domain.port.ProductSnapshotPort.ProductOrderSnapshot;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProductSnapshotPortImpl implements ProductSnapshotPort {

    private final ProductRepository productRepository;

    public ProductSnapshotPortImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<ProductOrderSnapshot> getOrderableSnapshot(ProductId productId) {
        Optional<Product> product = productRepository.findById(productId);
        return product.map(this::toSnapshot);
    }

    private ProductOrderSnapshot toSnapshot(Product product) {
        return new ProductOrderSnapshot(
                product.getId(),
                product.getSellerId(),
                product.getPrice(),
                product.getStatus(),
                product.getStock(),
                product.getVersion() != null ? product.getVersion().value().longValue() : null,
                product.getLocation() != null ? product.getLocation().value() : null
        );
    }
}
