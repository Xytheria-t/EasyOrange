package com.cartethyia.easyorange.favorite.domain.port;

import com.cartethyia.easyorange.favorite.domain.valueobject.ProductDetailInfo;
import com.cartethyia.easyorange.favorite.domain.valueobject.ProductInfo;
import com.cartethyia.easyorange.favorite.domain.valueobject.SellerInfo;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ProductInfoPort {

    boolean productExists(String productId);

    boolean isOwnProduct(String userId, String productId);

    List<ProductInfo> findProductsByIds(List<String> productIds);

    Map<String, SellerInfo> findSellersByIds(Set<String> sellerIds);

    List<ProductDetailInfo> assembleProductDetails(List<ProductInfo> products, Map<String, SellerInfo> sellerMap);
}
