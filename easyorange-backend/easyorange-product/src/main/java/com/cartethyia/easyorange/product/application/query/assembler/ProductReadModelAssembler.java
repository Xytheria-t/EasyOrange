package com.cartethyia.easyorange.product.application.query.assembler;

import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ProductReadModelAssembler {

    public record AssemblyContext(
            Map<String, List<ProductQueryRepository.ProductImageInfo>> imagesByProduct,
            Map<String, ProductQueryRepository.CategoryInfo> categoryMap,
            Map<String, ProductQueryRepository.ProductDetailInfo> detailMap,
            Map<String, SellerReadModel> sellerMap
    ) {}

    public ProductVO toProductVO(Product product, AssemblyContext ctx) {
        var builder = ProductVO.builder()
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
                .updateTime(product.getUpdateTime())
                .statusDesc(product.getStatus().getDesc())
                .conditionDesc(product.getConditionLevel() != null ? product.getConditionLevel().getDesc() : null);

        enrichSeller(builder, product.getSellerId().value(), ctx.sellerMap);
        enrichDetail(builder, product, ctx.detailMap);
        enrichCategory(builder, product.getCategoryId(), ctx.categoryMap);
        enrichImages(builder, product, ctx.imagesByProduct);

        return builder.build();
    }

    public ProductVO toProductVO(ProductReadModel readModel,
                                  Map<String, List<ProductQueryRepository.ProductImageInfo>> imagesByProduct,
                                  Map<String, SellerReadModel> sellerMap) {
        var builder = applyReadModelFields(ProductVO.builder(), readModel);
        enrichSeller(builder, readModel.sellerId(), sellerMap);
        enrichImages(builder, readModel.id(), imagesByProduct);
        return builder.build();
    }

    private static ProductVO.ProductVOBuilder applyReadModelFields(ProductVO.ProductVOBuilder builder, ProductReadModel m) {
        return builder
                .id(m.id()).sellerId(m.sellerId()).categoryId(m.categoryId())
                .title(m.title()).description(m.description())
                .price(m.price()).originalPrice(m.originalPrice()).stock(m.stock())
                .status(m.status()).statusDesc(m.statusDesc()).views(m.views())
                .condition(m.condition()).conditionDesc(m.conditionDesc())
                .location(m.location()).contactMethod(m.contactMethod())
                .images(m.images()).mainImageUrl(m.mainImageUrl())
                .username(m.username()).userAvatar(m.userAvatar())
                .categoryName(m.categoryName())
                .createTime(m.createTime()).updateTime(m.updateTime());
    }

    private static void enrichSeller(ProductVO.ProductVOBuilder builder, String sellerId,
                                      Map<String, SellerReadModel> sellerMap) {
        var seller = sellerMap.get(sellerId);
        if (seller != null) {
            builder.username(seller.nickName() != null ? seller.nickName() : seller.username());
            builder.userAvatar(seller.avatar());
        }
    }

    private static void enrichCategory(ProductVO.ProductVOBuilder builder, CategoryId categoryId,
                                        Map<String, ProductQueryRepository.CategoryInfo> categoryMap) {
        if (categoryId != null) {
            var category = categoryMap.get(categoryId.value());
            if (category != null) {
                builder.categoryName(category.name());
            }
        }
    }

    private static void enrichDetail(ProductVO.ProductVOBuilder builder, Product product,
                                      Map<String, ProductQueryRepository.ProductDetailInfo> detailMap) {
        var detail = detailMap.get(product.getId().value());
        if (detail != null) {
            builder.description(detail.description());
        } else if (product.getDescription() != null && !product.getDescription().isBlank()) {
            builder.description(product.getDescription().value());
        }
    }

    private static void enrichImages(ProductVO.ProductVOBuilder builder, Product product,
                                      Map<String, List<ProductQueryRepository.ProductImageInfo>> imagesByProduct) {
        var images = product.getImages();
        if (images != null && !images.isEmpty()) {
            var urls = images.imageUrls();
            builder.images(urls);
            builder.mainImageUrl(images.mainImage() != null ? images.mainImage().value() : urls.getFirst());
        } else {
            enrichImages(builder, product.getId().value(), imagesByProduct);
        }
    }

    private static void enrichImages(ProductVO.ProductVOBuilder builder, String productId,
                                      Map<String, List<ProductQueryRepository.ProductImageInfo>> imagesByProduct) {
        var images = imagesByProduct.getOrDefault(productId, List.of());
        if (images.isEmpty()) return;
        var urls = images.stream()
                .map(ProductQueryRepository.ProductImageInfo::imageUrl)
                .filter(Objects::nonNull)
                .toList();
        builder.images(urls);
        var mainImageUrl = images.stream()
                .filter(ProductQueryRepository.ProductImageInfo::isMain)
                .findFirst()
                .map(ProductQueryRepository.ProductImageInfo::imageUrl)
                .orElse(null);
        builder.mainImageUrl(mainImageUrl != null ? mainImageUrl : urls.getFirst());
    }
}
