package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.valueobject.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProductDataMapper {

    public ProductDO toDataObject(Product p) {
        return ProductDO.builder()
                .id(val(p.getId()))
                .userId(val(p.getSellerId()))
                .categoryId(val(p.getCategoryId()))
                .name(val(p.getTitle()))
                .price(val(p.getPrice()))
                .originalPrice(val(p.getOriginalPrice()))
                .stock(val(p.getStock()))
                .version(val(p.getVersion()))
                .status(p.getStatus())
                .viewCount(p.getViewCount())
                .conditionLevel(p.getConditionLevel())
                .location(val(p.getLocation()))
                .contactMethod(val(p.getContactMethod()))
                .searchText(p.getSearchText())
                .priceUpdateTime(p.getPriceUpdateTime())
                .createTime(p.getCreateTime())
                .updateTime(p.getUpdateTime())
                .build();
    }

    public ProductDetailDO toDetailDO(ProductId productId, ProductDescription description) {
        if (description == null || description.isBlank()) return null;
        return ProductDetailDO.builder()
                .productId(productId.value())
                .description(description.value())
                .build();
    }

    public List<ProductImageDO> toImageDOs(ProductId productId, ImageSet imageSet) {
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

    public Product toDomain(ProductDO productDO, ProductDetailDO detailDO, List<ProductImageDO> imageDOs) {
        return Product.builder()
                .id(ProductId.of(productDO.getId()))
                .sellerId(SellerId.of(productDO.getUserId()))
                .categoryId(CategoryId.of(productDO.getCategoryId()))
                .title(ProductTitle.of(productDO.getName()))
                .price(Money.of(productDO.getPrice()))
                .originalPrice(productDO.getOriginalPrice() != null ? Money.of(productDO.getOriginalPrice()) : null)
                .stock(StockQuantity.of(productDO.getStock()))
                .version(Version.of(productDO.getVersion()))
                .status(productDO.getStatus())
                .viewCount(productDO.getViewCount() != null ? productDO.getViewCount() : 0)
                .conditionLevel(productDO.getConditionLevel())
                .location(TradeLocation.of(productDO.getLocation()))
                .contactMethod(ContactMethod.of(productDO.getContactMethod()))
                .description(detailDO != null ? ProductDescription.of(detailDO.getDescription()) : null)
                .images(toImageSet(imageDOs))
                .tags(TagSet.empty())
                .searchText(productDO.getSearchText())
                .priceUpdateTime(productDO.getPriceUpdateTime())
                .createTime(productDO.getCreateTime())
                .updateTime(productDO.getUpdateTime())
                .build();
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

    // -- null-safe helpers for value objects --

    private static String val(ProductId v) { return v != null ? v.value() : null; }
    private static String val(SellerId v) { return v != null ? v.value() : null; }
    private static String val(CategoryId v) { return v != null ? v.value() : null; }
    private static String val(ProductTitle v) { return v != null ? v.value() : null; }
    private static String val(TradeLocation v) { return v != null ? v.value() : null; }
    private static BigDecimal val(Money v) { return v != null ? v.value() : null; }
    private static Integer val(StockQuantity v) { return v != null ? v.value() : null; }
    private static Integer val(Version v) { return v != null ? v.value() : null; }
    private static String val(ContactMethod v) { return v != null && v.isNotBlank() ? v.value() : null; }
}
