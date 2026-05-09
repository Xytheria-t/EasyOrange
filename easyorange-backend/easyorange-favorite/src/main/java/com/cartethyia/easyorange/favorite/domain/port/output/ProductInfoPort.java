package com.cartethyia.easyorange.favorite.domain.port.output;

import com.cartethyia.easyorange.favorite.domain.valueobject.ProductDetailInfo;
import com.cartethyia.easyorange.favorite.domain.valueobject.ProductInfo;
import com.cartethyia.easyorange.favorite.domain.valueobject.SellerInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ProductInfoPort extends OutboundPort {

    boolean productExists(Long productId);

    boolean isOwnProduct(Long userId, Long productId);

    List<ProductInfo> findProductsByIds(List<Long> productIds);

    Map<Long, SellerInfo> findSellersByIds(Set<Long> sellerIds);

    List<ProductDetailInfo> assembleProductDetails(List<ProductInfo> products, Map<Long, SellerInfo> sellerMap);
}
