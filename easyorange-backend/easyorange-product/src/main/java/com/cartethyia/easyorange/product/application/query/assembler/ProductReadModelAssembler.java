package com.cartethyia.easyorange.product.application.query.assembler;

import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ProductReadModelAssembler {

    /**
     * 从聚合根富化装配 ProductVO（含卖家/图片/分类/详情 多维关联）。
     */
    public ProductVO toProductVO(Product product,
                                  Map<String, List<ProductQueryRepository.ProductImageInfo>> imagesByProduct,
                                  Map<String, ProductQueryRepository.CategoryInfo> categoryMap,
                                  Map<String, ProductQueryRepository.ProductDetailInfo> detailMap,
                                  Map<String, SellerReadModel> sellerMap) {
        ProductVO.ProductVOBuilder builder = ProductVO.builder()
                .id(product.getId().value())
                .sellerId(product.getSellerId().value())
                .categoryId(product.getCategoryId() != null ? product.getCategoryId().value() : null)
                .title(product.getTitle().value())
                .price(product.getPrice().value())
                .originalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice().value() : null)
                .stock(product.getStock().value())
                .status(product.getStatus().getCode())
                .views(product.getViewCount())
                .condition(product.getConditionLevel() != null ? product.getConditionLevel().getCode() : null)
                .location(MaskUtils.maskAddress(
                        product.getLocation() != null ? product.getLocation().value() : null, 6))
                .contactMethod(product.getContactMethod() != null && product.getContactMethod().isNotBlank()
                        ? MaskUtils.maskPhone(product.getContactMethod().value()) : null)
                .createTime(product.getCreateTime())
                .updateTime(product.getUpdateTime());

        SellerReadModel seller = sellerMap.get(product.getSellerId().value());
        if (seller != null) {
            builder.username(seller.nickName() != null ? seller.nickName() : seller.username());
            builder.userAvatar(seller.avatar());
        }

        ProductQueryRepository.ProductDetailInfo detail = detailMap.get(product.getId().value());
        if (detail != null) {
            builder.description(detail.description());
        } else if (product.getDescription() != null && !product.getDescription().isBlank()) {
            builder.description(product.getDescription().value());
        }

        builder.statusDesc(product.getStatus().getDesc());
        if (product.getConditionLevel() != null) {
            builder.conditionDesc(product.getConditionLevel().getDesc());
        }

        if (product.getCategoryId() != null) {
            ProductQueryRepository.CategoryInfo category = categoryMap.get(product.getCategoryId().value());
            if (category != null) {
                builder.categoryName(category.name());
            }
        }

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            List<String> imageUrls = product.getImages().imageUrls();
            builder.images(imageUrls);
            if (product.getImages().mainImage() != null) {
                builder.mainImageUrl(product.getImages().mainImage().value());
            } else if (!imageUrls.isEmpty()) {
                builder.mainImageUrl(imageUrls.get(0));
            }
        } else {
            List<ProductQueryRepository.ProductImageInfo> images = imagesByProduct.getOrDefault(product.getId().value(), List.of());
            if (!images.isEmpty()) {
                List<String> imageUrls = images.stream()
                        .map(ProductQueryRepository.ProductImageInfo::imageUrl)
                        .collect(Collectors.toList());
                builder.images(imageUrls);
                images.stream()
                        .filter(ProductQueryRepository.ProductImageInfo::isMain)
                        .findFirst()
                        .ifPresent(img -> builder.mainImageUrl(img.imageUrl()));
            }
        }

        return builder.build();
    }

    public ProductVO toProductVO(ProductReadModel readModel) {
        return ProductVO.builder()
                .id(readModel.id())
                .sellerId(readModel.sellerId())
                .username(readModel.username())
                .userAvatar(readModel.userAvatar())
                .categoryId(readModel.categoryId())
                .categoryName(readModel.categoryName())
                .title(readModel.title())
                .description(readModel.description())
                .price(readModel.price())
                .originalPrice(readModel.originalPrice())
                .stock(readModel.stock())
                .status(readModel.status())
                .statusDesc(readModel.statusDesc())
                .views(readModel.views())
                .condition(readModel.condition())
                .conditionDesc(readModel.conditionDesc())
                .location(readModel.location())
                .contactMethod(readModel.contactMethod())
                .images(readModel.images())
                .mainImageUrl(readModel.mainImageUrl())
                .createTime(readModel.createTime())
                .updateTime(readModel.updateTime())
                .build();
    }

    /**
     * 从 ReadModel + 关联数据富化装配 ProductVO（重写卖家信息与图片列表）。
     */
    public ProductVO toProductVO(ProductReadModel readModel,
                                  Map<String, List<ProductQueryRepository.ProductImageInfo>> imagesByProduct,
                                  Map<String, SellerReadModel> sellerMap) {
        var seller = sellerMap.get(readModel.sellerId());
        var username = seller != null
                ? (seller.nickName() != null ? seller.nickName() : seller.username())
                : readModel.username();
        var userAvatar = seller != null ? seller.avatar() : readModel.userAvatar();

        var images = imagesByProduct.getOrDefault(readModel.id(), List.of());
        var imageUrls = images.stream()
                .map(ProductQueryRepository.ProductImageInfo::imageUrl)
                .filter(Objects::nonNull)
                .toList();
        var mainImageUrl = images.stream()
                .filter(ProductQueryRepository.ProductImageInfo::isMain)
                .findFirst()
                .map(ProductQueryRepository.ProductImageInfo::imageUrl)
                .orElseGet(() -> imageUrls.isEmpty() ? "" : imageUrls.getFirst());

        return ProductVO.builder()
                .id(readModel.id())
                .sellerId(readModel.sellerId())
                .username(username)
                .userAvatar(userAvatar)
                .categoryId(readModel.categoryId())
                .categoryName(readModel.categoryName())
                .title(readModel.title())
                .description(readModel.description())
                .price(readModel.price())
                .originalPrice(readModel.originalPrice())
                .stock(readModel.stock())
                .status(readModel.status())
                .statusDesc(readModel.statusDesc())
                .views(readModel.views())
                .condition(readModel.condition())
                .conditionDesc(readModel.conditionDesc())
                .location(readModel.location())
                .contactMethod(readModel.contactMethod())
                .images(imageUrls)
                .mainImageUrl(mainImageUrl)
                .createTime(readModel.createTime())
                .updateTime(readModel.updateTime())
                .build();
    }
}
