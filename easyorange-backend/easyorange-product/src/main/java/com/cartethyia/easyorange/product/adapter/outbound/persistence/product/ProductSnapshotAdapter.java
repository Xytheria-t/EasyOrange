package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.port.ProductSnapshotPort;
import com.cartethyia.easyorange.product.domain.port.ProductSnapshotPort.ProductSnapshot;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.ImageUrl;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class ProductSnapshotAdapter implements ProductSnapshotPort {

    private final ProductRepository productRepository;

    public ProductSnapshotAdapter(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<ProductSnapshot> findSnapshots(List<ProductId> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return productRepository.findByIds(productIds).stream()
                .map(this::toSnapshot)
                .toList();
    }

    private ProductSnapshot toSnapshot(Product product) {
        return new ProductSnapshot(
                product.getId(),
                product.getSellerId(),
                product.getPrice(),
                product.getStatus(),
                product.getStock(),
                product.getTitle() != null ? product.getTitle().value() : null,
                mainImage(product.getImages()),
                product.getDescription() != null ? product.getDescription().value() : null,
                product.getConditionLevel() != null
                        ? product.getConditionLevel().getDesc()
                        : null);
    }

    /** 主图优先，无主图取第一张，无图返回 null。 */
    private static String mainImage(ImageSet images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        ImageUrl main = images.mainImage();
        if (main != null) {
            return main.value();
        }
        return images.imageUrls().stream().filter(Objects::nonNull).findFirst().orElse(null);
    }
}
