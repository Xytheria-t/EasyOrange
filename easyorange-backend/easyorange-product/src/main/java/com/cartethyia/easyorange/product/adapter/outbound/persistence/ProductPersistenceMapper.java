package com.cartethyia.easyorange.product.adapter.outbound.persistence;

import com.cartethyia.easyorange.product.domain.aggregate.ProductAggregate;
import com.cartethyia.easyorange.product.entity.Product;
import com.cartethyia.easyorange.product.entity.ProductDetail;
import com.cartethyia.easyorange.product.entity.ProductImage;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductPersistenceMapper {

    public ProductAggregate toAggregate(Product product, ProductDetail detail, List<ProductImage> images) {
        if (product == null) {
            return null;
        }
        return ProductAggregate.load(product, detail, images);
    }

    public Product toPersistence(ProductAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }
        return aggregate.getProduct();
    }

    public ProductDetail toDetailPersistence(ProductAggregate aggregate) {
        if (aggregate == null || aggregate.getDetail() == null) {
            return null;
        }
        return aggregate.getDetail();
    }

    public List<ProductImage> toImagePersistenceList(ProductAggregate aggregate) {
        if (aggregate == null || aggregate.getImages() == null || aggregate.getImages().isEmpty()) {
            return Collections.emptyList();
        }
        return aggregate.getImages().stream()
                .map(img -> {
                    ProductImage image = new ProductImage();
                    image.setProductId(aggregate.getId());
                    image.setImageUrl(img.getImageUrl());
                    image.setSortOrder(img.getSortOrder());
                    image.setIsMain(img.getIsMain());
                    return image;
                })
                .collect(Collectors.toList());
    }
}