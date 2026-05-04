package com.cartethyia.easyorange.favorite.infrastructure.acl;

import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductAclServiceImpl implements ProductAclService {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;

    public ProductAclServiceImpl(ProductRepository productRepository,
                                  ProductQueryRepository productQueryRepository) {
        this.productRepository = productRepository;
        this.productQueryRepository = productQueryRepository;
    }

    @Override
    public boolean productExists(Long productId) {
        return productRepository.findById(ProductId.of(productId)).isPresent();
    }

    @Override
    public boolean isOwnProduct(Long userId, Long productId) {
        return productRepository.findById(ProductId.of(productId))
                .map(p -> p.getSellerId().value().equals(userId))
                .orElse(false);
    }

    @Override
    public List<ProductReadModel> findProductsByIds(List<Long> productIds) {
        return productQueryRepository.findProductsByIds(productIds);
    }

    @Override
    public Map<Long, SellerReadModel> findSellersByIds(Set<Long> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productQueryRepository.findSellersByIds(sellerIds).stream()
                .collect(Collectors.toMap(SellerReadModel::id, s -> s, (a, b) -> a));
    }

    @Override
    public List<ProductVO> assembleProductVOs(List<ProductReadModel> products,
                                               Map<Long, SellerReadModel> sellerMap) {
        List<Long> productIds = products.stream()
                .map(ProductReadModel::id)
                .collect(Collectors.toList());

        Map<Long, List<ProductQueryRepository.ProductImageInfo>> imagesByProduct = productQueryRepository
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

                    ProductVO.ProductVOBuilder builder = ProductVO.builder()
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
                        SellerReadModel seller = sellerMap.get(product.sellerId());
                        if (seller != null) {
                            builder.username(seller.nickName() != null ? seller.nickName() : seller.username());
                            builder.userAvatar(seller.avatar());
                        }
                    }
                    return builder.build();
                })
                .collect(Collectors.toList());
    }
}
