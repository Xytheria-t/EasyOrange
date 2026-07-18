package com.cartethyia.easyorange.product.adapter.outbound.persistence.converter;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductImageDO;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.ImageUrl;
import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TagSet;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;
import com.cartethyia.easyorange.product.domain.valueobject.Version;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductConverter {

    // ==================== Domain → DO ====================

    @Mapping(target = "userId", source = "sellerId")
    @Mapping(target = "name", source = "title")
    @Mapping(target = "tags", ignore = true)
    ProductDO toDataObject(Product product);

    default ProductDetailDO toDetailDO(ProductId productId, ProductDescription description) {
        if (description == null || description.isBlank()) return null;
        return ProductDetailDO.builder()
                .productId(productId.value())
                .description(description.value())
                .build();
    }

    default List<ProductImageDO> toImageDOs(ProductId productId, ImageSet imageSet) {
        if (imageSet == null || imageSet.isEmpty()) return List.of();
        var urls = imageSet.imageUrls();
        var result = new ArrayList<ProductImageDO>(urls.size());
        for (int i = 0; i < urls.size(); i++) {
            result.add(ProductImageDO.builder()
                    .productId(productId.value())
                    .imageUrl(urls.get(i))
                    .sortOrder(i)
                    .isMain(i == 0 ? 1 : 0)
                    .build());
        }
        return result;
    }

    // ==================== DO → Domain ====================

    default Product toDomain(ProductDO productDO, ProductDetailDO detailDO, List<ProductImageDO> imageDOs) {
        return Product.reconstitute(
                ProductId.of(productDO.getId()),
                SellerId.of(productDO.getUserId()),
                CategoryId.of(productDO.getCategoryId()),
                ProductTitle.of(productDO.getName()),
                Money.of(productDO.getPrice()),
                productDO.getOriginalPrice() != null ? Money.of(productDO.getOriginalPrice()) : null,
                StockQuantity.of(productDO.getStock()),
                Version.of(productDO.getVersion()),
                ProductStatus.fromCode(productDO.getStatus()),
                productDO.getViewCount(),
                ConditionLevel.fromCode(productDO.getConditionLevel()),
                TradeLocation.of(productDO.getLocation()),
                ContactMethod.of(productDO.getContactMethod()),
                detailDO != null ? ProductDescription.of(detailDO.getDescription()) : null,
                toImageSet(imageDOs),
                TagSet.empty(),
                productDO.getSearchText(),
                productDO.getPriceUpdateTime(),
                productDO.getCreateTime(),
                productDO.getUpdateTime()
        );
    }

    private static ImageSet toImageSet(List<ProductImageDO> imageDOs) {
        if (imageDOs == null || imageDOs.isEmpty()) return ImageSet.empty();
        return ImageSet.ofImages(imageDOs.stream()
                .map(img -> new ImageSet.ProductImage(
                        new ImageUrl(img.getImageUrl()),
                        img.getSortOrder(),
                        img.getIsMain() != null && img.getIsMain().equals(1)))
                .toList());
    }

    // ==================== Type mappings for MapStruct (used by toDataObject) ====================

    default String map(ProductId id) { return id != null ? id.value() : null; }

    default String map(SellerId id) { return id != null ? id.value() : null; }

    default String map(CategoryId id) { return id != null ? id.value() : null; }

    default String map(ProductTitle t) { return t != null ? t.value() : null; }

    default BigDecimal map(Money m) { return m != null ? m.value() : null; }

    default Integer map(StockQuantity q) { return q != null ? q.value() : null; }

    default Integer map(Version v) { return v != null ? v.value() : null; }

    default Integer map(ProductStatus s) { return s != null ? s.getCode() : null; }

    default Integer map(ConditionLevel c) { return c != null ? c.getCode() : null; }

    default String map(TradeLocation l) { return l != null ? l.value() : null; }

    default String map(ContactMethod cm) { return cm != null && cm.isNotBlank() ? cm.value() : null; }
}
