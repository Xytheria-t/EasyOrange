package com.cartethyia.easyorange.product.adapter.outbound.persistence;

import com.cartethyia.easyorange.product.domain.aggregate.ProductAggregate;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ConditionLevelVO;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.Money;
import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductStatusVO;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TagSet;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;
import com.cartethyia.easyorange.product.domain.valueobject.Version;
import com.cartethyia.easyorange.product.entity.Product;
import com.cartethyia.easyorange.product.entity.ProductDetail;
import com.cartethyia.easyorange.product.entity.ProductImage;
import com.cartethyia.easyorange.product.enums.ProductStatus;
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

        ProductId id = new ProductId(product.getId());
        SellerId sellerId = new SellerId(product.getUserId());
        CategoryId categoryId = product.getCategoryId() != null ? new CategoryId(product.getCategoryId()) : null;
        ProductTitle name = new ProductTitle(product.getName());
        Money price = new Money(product.getPrice());
        Money originalPrice = product.getOriginalPrice() != null ? new Money(product.getOriginalPrice()) : null;
        StockQuantity stock = new StockQuantity(product.getStock());
        ProductStatusVO status = new ProductStatusVO(ProductStatus.fromCode(product.getStatus()));
        Integer viewCount = product.getViewCount();
        ConditionLevelVO conditionLevel = product.getConditionLevel() != null
                ? new ConditionLevelVO(com.cartethyia.easyorange.product.enums.ConditionLevel.fromCode(product.getConditionLevel()))
                : null;
        TradeLocation location = new TradeLocation(product.getLocation());
        ContactMethod contactMethod = new ContactMethod(product.getContactMethod());
        TagSet tags = TagSet.empty();
        ProductDescription description = detail != null ? new ProductDescription(detail.getDescription()) : new ProductDescription(null);
        ImageSet imageSet = images != null && !images.isEmpty()
                ? toImageSet(images)
                : ImageSet.empty();
        Version version = new Version(product.getVersion() != null ? product.getVersion() : 0);

        return ProductAggregate.load(
                id, sellerId, categoryId,
                name.value(), price.value(), originalPrice != null ? originalPrice.value() : null,
                stock, status, viewCount, conditionLevel,
                location, contactMethod, tags,
                description, imageSet, version
        );
    }

    public Product toPersistence(ProductAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }

        Product product = new Product();
        if (aggregate.getId() != null) {
            product.setId(aggregate.getId().value());
        }
        product.setUserId(aggregate.getSellerId().value());
        product.setCategoryId(aggregate.getCategoryId() != null ? aggregate.getCategoryId().value() : null);
        product.setName(aggregate.getName().value());
        product.setPrice(aggregate.getPrice().value());
        product.setOriginalPrice(aggregate.getOriginalPrice() != null ? aggregate.getOriginalPrice().value() : null);
        product.setStock(aggregate.getStock().value());
        product.setStatus(aggregate.getStatus().value().getCode());
        product.setViewCount(aggregate.getViewCount());
        product.setConditionLevel(aggregate.getConditionLevel() != null ? aggregate.getConditionLevel().value().getCode() : null);
        product.setLocation(aggregate.getLocation().value());
        product.setContactMethod(aggregate.getContactMethod().value());
        product.setVersion(aggregate.getVersion().value());

        return product;
    }

    public ProductDetail toDetailPersistence(ProductAggregate aggregate) {
        if (aggregate == null || aggregate.getId() == null) {
            return null;
        }
        if (aggregate.getDescription() == null || aggregate.getDescription().isBlank()) {
            return null;
        }

        ProductDetail detail = new ProductDetail();
        detail.setProductId(aggregate.getId().value());
        detail.setDescription(aggregate.getDescription().value());
        return detail;
    }

    public List<ProductImage> toImagePersistenceList(ProductAggregate aggregate) {
        if (aggregate == null || aggregate.getImages() == null || aggregate.getImages().isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductImage> images = aggregate.getImages().images().stream()
                .map(vo -> {
                    ProductImage image = new ProductImage();
                    if (aggregate.getId() != null) {
                        image.setProductId(aggregate.getId().value());
                    }
                    image.setImageUrl(vo.url() != null ? vo.url().value() : null);
                    image.setSortOrder(vo.sortOrder());
                    image.setIsMain(vo.isMain() ? 1 : 0);
                    return image;
                })
                .collect(Collectors.toList());

        return images;
    }

    private ImageSet toImageSet(List<ProductImage> images) {
        List<ImageSet.ProductImageVO> voList = images.stream()
                .map(img -> new ImageSet.ProductImageVO(
                        img.getImageUrl() != null ? new com.cartethyia.easyorange.product.domain.valueobject.ImageUrl(img.getImageUrl()) : null,
                        img.getSortOrder(),
                        img.getIsMain() != null && img.getIsMain() == 1
                ))
                .collect(Collectors.toList());
        return new ImageSet(voList);
    }
}
