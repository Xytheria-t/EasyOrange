package com.cartethyia.easyorange.product.adapter.outbound.product;

import com.cartethyia.easyorange.product.application.port.outbound.ProductSnapshotPort;
import com.cartethyia.easyorange.product.application.port.outbound.ProductSnapshotPort.ProductOrderSnapshot;
import com.cartethyia.easyorange.product.domain.aggregate.ProductAggregate;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.Money;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductStatusVO;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.entity.Product;
import com.cartethyia.easyorange.product.enums.ProductStatus;
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
        Product product = productRepository.findById(productId.value());
        if (product == null) {
            return Optional.empty();
        }
        return Optional.of(toSnapshot(product));
    }

    private ProductOrderSnapshot toSnapshot(Product product) {
        return new ProductOrderSnapshot(
                new ProductId(product.getId()),
                new SellerId(product.getUserId()),
                new Money(product.getPrice()),
                new ProductStatusVO(ProductStatus.fromCode(product.getStatus())),
                new StockQuantity(product.getStock()),
                product.getVersion() != null ? product.getVersion().longValue() : null
        );
    }
}