package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.port.ProductSnapshotPort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class ProductSnapshotPortImpl implements ProductSnapshotPort {

    private final ProductRepository productRepository;

    public ProductSnapshotPortImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<ProductSnapshot> findSnapshot(ProductId productId) {
        return productRepository.findById(productId).map(this::toSnapshot);
    }

    private ProductSnapshot toSnapshot(Product product) {
        return new ProductSnapshot(
                product.getId(),
                product.getSellerId(),
                product.getPrice(),
                product.getStatus(),
                product.getStock(),
                product.getLocation() != null ? product.getLocation().value() : null);
    }
}
