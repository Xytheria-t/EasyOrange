package com.cartethyia.easyorange.product.assembler;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.product.constant.ProductConstants;
import com.cartethyia.easyorange.product.dto.vo.SellerInfo;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import com.cartethyia.easyorange.product.entity.Category;
import com.cartethyia.easyorange.product.entity.Product;
import com.cartethyia.easyorange.product.entity.ProductDetail;
import com.cartethyia.easyorange.product.entity.ProductImage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ProductAssembler {

    public Page<ProductVO> toProductVOPage(List<Product> products,
                                            Map<Long, List<ProductImage>> imagesByProduct,
                                            Map<Long, Category> categoryMap,
                                            Map<Long, ProductDetail> detailMap,
                                            Map<Long, SellerInfo> sellerMap,
                                            int total, long current, long size) {
        if (products.isEmpty()) {
            Page<ProductVO> emptyPage = new Page<>(current, size, total);
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        List<ProductVO> voList = products.stream()
                .map(p -> toProductVO(p, imagesByProduct, categoryMap, detailMap, sellerMap))
                .collect(Collectors.toList());

        Page<ProductVO> voPage = new Page<>(current, size, total);
        voPage.setRecords(voList);
        return voPage;
    }

    public ProductVO toProductVO(Product product,
                                  Map<Long, List<ProductImage>> imagesByProduct,
                                  Map<Long, Category> categoryMap,
                                  Map<Long, ProductDetail> detailMap,
                                  Map<Long, SellerInfo> sellerMap) {
        ProductVO.ProductVOBuilder builder = ProductVO.builder()
                .id(product.getId())
                .sellerId(product.getUserId())
                .categoryId(product.getCategoryId())
                .title(product.getName())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .stock(product.getStock())
                .status(product.getStatus())
                .views(product.getViewCount())
                .condition(product.getConditionLevel())
                .location(MaskUtils.maskAddress(product.getLocation(), 6))
                .contactMethod(MaskUtils.maskPhone(product.getContactMethod()))
                .createTime(product.getCreateTime())
                .updateTime(product.getUpdateTime());

        SellerInfo seller = sellerMap.get(product.getUserId());
        if (seller != null) {
            builder.username(seller.getNickName() != null ? seller.getNickName() : seller.getUsername());
        }

        ProductDetail detail = detailMap.get(product.getId());
        if (detail != null) {
            builder.description(detail.getDescription());
        }

        com.cartethyia.easyorange.product.enums.ProductStatus status =
                com.cartethyia.easyorange.product.enums.ProductStatus.fromCode(product.getStatus());
        if (status != null) {
            builder.statusDesc(status.getDesc());
        }

        com.cartethyia.easyorange.product.enums.ConditionLevel condition =
                com.cartethyia.easyorange.product.enums.ConditionLevel.fromCode(product.getConditionLevel());
        if (condition != null) {
            builder.conditionDesc(condition.getDesc());
        }

        if (product.getCategoryId() != null) {
            Category category = categoryMap.get(product.getCategoryId());
            if (category != null) {
                builder.categoryName(category.getName());
            }
        }

        List<ProductImage> images = imagesByProduct.getOrDefault(product.getId(), List.of());
        if (!images.isEmpty()) {
            List<String> imageUrls = images.stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList());
            builder.images(imageUrls);

            ProductImage mainImage = images.stream()
                    .filter(img -> img.getIsMain() != null && img.getIsMain().equals(ProductConstants.IMAGE_IS_MAIN))
                    .findFirst()
                    .orElse(images.get(0));
            builder.mainImageUrl(mainImage.getImageUrl());
        }

        return builder.build();
    }

    public List<Long> collectCategoryIds(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        return products.stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    public Map<Long, List<ProductImage>> groupImagesByProduct(List<ProductImage> images) {
        return images.stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId));
    }

    public Map<Long, Category> buildCategoryMap(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return Map.of();
        }
        return categories.stream()
                .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));
    }

    public Map<Long, ProductDetail> buildDetailMap(List<ProductDetail> details) {
        if (details == null || details.isEmpty()) {
            return Map.of();
        }
        return details.stream()
                .collect(Collectors.toMap(ProductDetail::getProductId, d -> d, (a, b) -> a));
    }

    public Map<Long, SellerInfo> buildSellerMap(List<SellerInfo> sellers) {
        if (sellers == null || sellers.isEmpty()) {
            return Map.of();
        }
        return sellers.stream()
                .collect(Collectors.toMap(SellerInfo::getId, s -> s, (a, b) -> a));
    }
}
