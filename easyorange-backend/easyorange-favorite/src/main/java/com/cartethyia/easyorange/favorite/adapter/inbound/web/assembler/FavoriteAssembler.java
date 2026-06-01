package com.cartethyia.easyorange.favorite.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.response.FavoriteResponse;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import com.cartethyia.easyorange.favorite.domain.port.ProductInfoPort;
import com.cartethyia.easyorange.favorite.domain.valueobject.ProductDetailInfo;
import com.cartethyia.easyorange.favorite.domain.valueobject.ProductInfo;
import com.cartethyia.easyorange.favorite.domain.valueobject.SellerInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FavoriteAssembler {

    private final ProductInfoPort productInfoPort;

    public PageResult<FavoriteResponse> toPageResult(PageResult<Favorite> page, int pageNum, int pageSize) {
        List<Favorite> favorites = page.records();
        if (favorites == null || favorites.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        List<Long> productIds = favorites.stream()
                .map(Favorite::getProductId)
                .collect(Collectors.toList());

        List<ProductInfo> products = productInfoPort.findProductsByIds(productIds);
        if (products.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        Set<Long> sellerIds = products.stream()
                .map(ProductInfo::sellerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, SellerInfo> sellerMap = productInfoPort.findSellersByIds(sellerIds);

        List<ProductDetailInfo> productDetailInfos = productInfoPort.assembleProductDetails(products, sellerMap);

        Map<Long, ProductDetailInfo> productDetailMap = productDetailInfos.stream()
                .collect(Collectors.toMap(ProductDetailInfo::id, p -> p, (a, b) -> a));

        Map<Long, Favorite> favoriteByProductId = favorites.stream()
                .collect(Collectors.toMap(Favorite::getProductId, f -> f, (a, b) -> a));

        List<FavoriteResponse> responses = productIds.stream()
                .map(productId -> {
                    Favorite fav = favoriteByProductId.get(productId);
                    ProductDetailInfo productDetail = productDetailMap.get(productId);
                    if (fav == null || productDetail == null) {
                        return null;
                    }
                    return FavoriteResponse.builder()
                            .id(fav.getId())
                            .productId(fav.getProductId())
                            .product(productDetail)
                            .createTime(fav.getCreateTime())
                            .build();
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());

        return PageResult.of(responses, page.total(), pageNum, pageSize);
    }
}