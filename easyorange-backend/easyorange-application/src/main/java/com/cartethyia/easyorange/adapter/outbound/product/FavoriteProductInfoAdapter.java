package com.cartethyia.easyorange.adapter.outbound.product;

import com.cartethyia.easyorange.favorite.domain.port.ProductInfoPort;
import com.cartethyia.easyorange.favorite.domain.valueobject.ProductDetailInfo;
import com.cartethyia.easyorange.favorite.domain.valueobject.ProductInfo;
import com.cartethyia.easyorange.favorite.domain.valueobject.SellerInfo;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FavoriteProductInfoAdapter implements ProductInfoPort {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;

    @Override
    public boolean productExists(String productId) {
        return productRepository.findById(ProductId.of(productId)).isPresent();
    }

    @Override
    public boolean isOwnProduct(String userId, String productId) {
        return productRepository.findById(ProductId.of(productId))
                .map(p -> p.getSellerId().value().equals(userId))
                .orElse(false);
    }

    @Override
    public List<ProductInfo> findProductsByIds(List<String> productIds) {
        List<ProductReadModel> products = productQueryRepository.findProductsByIds(productIds);
        return products.stream()
                .map(this::toProductInfo)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, SellerInfo> findSellersByIds(Set<String> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty()) {
            return Map.of();
        }
        return productQueryRepository.findSellersByIds(sellerIds).stream()
                .collect(Collectors.toMap(
                        SellerReadModel::id,
                        this::toSellerInfo,
                        (a, b) -> a
                ));
    }

    @Override
    public List<ProductDetailInfo> assembleProductDetails(List<ProductInfo> products,
                                                           Map<String, SellerInfo> sellerMap) {
        List<String> productIds = products.stream()
                .map(ProductInfo::id)
                .collect(Collectors.toList());

        Map<String, List<ProductQueryRepository.ProductImageInfo>> imagesByProduct = productQueryRepository
                .findImagesByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(ProductQueryRepository.ProductImageInfo::productId));

        return products.stream()
                .map(product -> {
                    List<ProductQueryRepository.ProductImageInfo> images = imagesByProduct.getOrDefault(product.id(), List.of());
                    List<String> imageUrls = images.stream()
                            .map(ProductQueryRepository.ProductImageInfo::imageUrl)
                            .collect(Collectors.toList());
                    String mainImageUrl = images.stream()
                            .filter(ProductQueryRepository.ProductImageInfo::isMain)
                            .findFirst()
                            .map(ProductQueryRepository.ProductImageInfo::imageUrl)
                            .orElse(imageUrls.isEmpty() ? "" : imageUrls.getFirst());

                    ProductDetailInfo.Builder builder = ProductDetailInfo.builder()
                            .id(product.id())
                            .sellerId(product.sellerId())
                            .categoryId(product.categoryId())
                            .title(product.title())
                            .description(product.description())
                            .price(product.price())
                            .originalPrice(product.originalPrice())
                            .stock(product.stock())
                            .status(product.status())
                            .statusDesc(product.statusDesc())
                            .views(product.views())
                            .condition(product.condition())
                            .conditionDesc(product.conditionDesc())
                            .location(product.location())
                            .contactMethod(product.contactMethod())
                            .images(imageUrls)
                            .mainImageUrl(mainImageUrl)
                            .createTime(product.createTime())
                            .updateTime(product.updateTime());

                    if (product.sellerId() != null) {
                        SellerInfo seller = sellerMap.get(product.sellerId());
                        if (seller != null) {
                            builder.username(seller.nickName() != null ? seller.nickName() : seller.username());
                            builder.userAvatar(seller.avatar());
                        }
                    }
                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    private ProductInfo toProductInfo(ProductReadModel model) {
        return new ProductInfo(
                model.id(),
                model.sellerId(),
                model.categoryId(),
                model.title(),
                model.description(),
                model.price(),
                model.originalPrice(),
                model.stock(),
                model.status(),
                model.statusDesc(),
                model.views(),
                model.condition(),
                model.conditionDesc(),
                model.location(),
                model.contactMethod(),
                model.images(),
                model.mainImageUrl(),
                model.createTime(),
                model.updateTime()
        );
    }

    private SellerInfo toSellerInfo(SellerReadModel model) {
        return new SellerInfo(
                model.id(),
                model.username(),
                model.nickName(),
                model.avatar()
        );
    }
}
