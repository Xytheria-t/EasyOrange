package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import com.cartethyia.easyorange.product.domain.port.ProductSnapshotPort;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Primary
@Component
public class ProductSnapshotPortImpl implements ProductSnapshotPort {

    private final ProductRepository productRepository;

    public ProductSnapshotPortImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<ProductOrderSnapshot> findSnapshot(ProductId productId) {
        return productRepository.findById(productId)
                .map(this::toSnapshot);
    }

    private ProductOrderSnapshot toSnapshot(Product product) {
        return new ProductOrderSnapshot(
                product.getId(),
                product.getSellerId(),
                product.getPrice(),
                product.getStatus(),
                product.getStock(),
                product.getLocation() != null ? product.getLocation().value() : null
        );
    }
}
