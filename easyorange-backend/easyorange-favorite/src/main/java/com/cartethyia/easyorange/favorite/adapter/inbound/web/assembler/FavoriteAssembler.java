package com.cartethyia.easyorange.favorite.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.response.FavoriteResponse;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import com.cartethyia.easyorange.favorite.domain.port.ProductInfoPort;
import com.cartethyia.easyorange.favorite.domain.valueobject.ProductDetailInfo;
import com.cartethyia.easyorange.favorite.domain.valueobject.ProductInfo;
import com.cartethyia.easyorange.favorite.domain.valueobject.SellerInfo;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FavoriteAssembler {

    private final ProductInfoPort productInfoPort;

    public PageResult<FavoriteResponse> toPageResult(PageResult<Favorite> page, int pageNum, int pageSize) {
        List<Favorite> favorites = page.records();
        if (favorites == null || favorites.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        List<String> productIds = favorites.stream().map(Favorite::productId).collect(Collectors.toList());

        List<ProductInfo> products = productInfoPort.findProductsByIds(productIds);
        if (products.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        Set<String> sellerIds = products.stream()
                .map(ProductInfo::sellerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<String, SellerInfo> sellerMap = productInfoPort.findSellersByIds(sellerIds);

        List<ProductDetailInfo> productDetailInfos = productInfoPort.assembleProductDetails(products, sellerMap);

        Map<String, ProductDetailInfo> productDetailMap =
                productDetailInfos.stream().collect(Collectors.toMap(ProductDetailInfo::id, p -> p, (a, b) -> a));

        Map<String, Favorite> favoriteByProductId =
                favorites.stream().collect(Collectors.toMap(Favorite::productId, f -> f, (a, b) -> a));

        List<FavoriteResponse> responses = productIds.stream()
                .map(productId -> {
                    Favorite fav = favoriteByProductId.get(productId);
                    ProductDetailInfo productDetail = productDetailMap.get(productId);
                    if (fav == null || productDetail == null) {
                        return null;
                    }
                    return FavoriteResponse.builder()
                            .id(fav.id())
                            .productId(fav.productId())
                            .product(productDetail)
                            .createTime(fav.createTime())
                            .build();
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());

        return PageResult.of(responses, page.total(), pageNum, pageSize);
    }
}
