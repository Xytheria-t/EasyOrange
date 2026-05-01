package com.cartethyia.easyorange.product.adapter.outbound.persistence.converter;

import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.ImageUrl;
import com.cartethyia.easyorange.product.domain.valueobject.Money;
import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TagSet;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;
import com.cartethyia.easyorange.product.domain.valueobject.Version;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductImageDO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public final class ProductConverter {

    public Product toDomain(ProductDO productDO, ProductDetailDO detailDO, List<ProductImageDO> imageDOs) {
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

    public ImageSet toImageSet(List<ProductImageDO> imageDOs) {
        if (imageDOs == null || imageDOs.isEmpty()) {
            return ImageSet.empty();
        }
        List<ImageSet.ProductImage> images = imageDOs.stream()
                .map(img -> new ImageSet.ProductImage(
                        new ImageUrl(img.getImageUrl()),
                        img.getSortOrder(),
                        img.getIsMain() != null && img.getIsMain().equals(1)
                ))
                .collect(Collectors.toList());
        return ImageSet.ofImages(images);
    }

    public ProductDO toDataObject(Product product) {
        ProductDO dobj = new ProductDO();
        dobj.setId(product.getId() != null ? product.getId().value() : null);
        dobj.setUserId(product.getSellerId().value());
        dobj.setCategoryId(product.getCategoryId() != null ? product.getCategoryId().value() : null);
        dobj.setName(product.getTitle().value());
        dobj.setPrice(product.getPrice().value());
        dobj.setOriginalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice().value() : null);
        dobj.setStock(product.getStock().value());
        dobj.setVersion(product.getVersion().value());
        dobj.setStatus(product.getStatus() != null ? product.getStatus().getCode() : null);
        dobj.setViewCount(product.getViewCount());
        dobj.setConditionLevel(product.getConditionLevel() != null ? product.getConditionLevel().getCode() : null);
        dobj.setLocation(product.getLocation() != null ? product.getLocation().value() : null);
        dobj.setContactMethod(product.getContactMethod() != null && product.getContactMethod().isNotBlank() ? product.getContactMethod().value() : null);
        dobj.setCreateTime(product.getCreateTime());
        dobj.setUpdateTime(product.getUpdateTime());
        dobj.setPriceUpdateTime(product.getPriceUpdateTime());
        return dobj;
    }

    public ProductDetailDO toDetailDO(ProductId productId, ProductDescription description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        ProductDetailDO dobj = new ProductDetailDO();
        dobj.setProductId(productId.value());
        dobj.setDescription(description.value());
        return dobj;
    }

    public List<ProductImageDO> toImageDOs(ProductId productId, ImageSet imageSet) {
        if (imageSet == null || imageSet.isEmpty()) {
            return List.of();
        }
        List<String> urls = imageSet.imageUrls();
        List<ProductImageDO> result = new ArrayList<>(urls.size());
        for (int i = 0; i < urls.size(); i++) {
            ProductImageDO img = new ProductImageDO();
            img.setProductId(productId.value());
            img.setImageUrl(urls.get(i));
            img.setSortOrder(i);
            img.setIsMain(i == 0 ? 1 : 0);
            result.add(img);
        }
        return result;
    }
}
