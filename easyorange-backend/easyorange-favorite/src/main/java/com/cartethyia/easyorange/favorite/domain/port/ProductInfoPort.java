package com.cartethyia.easyorange.favorite.domain.port;

import com.cartethyia.easyorange.favorite.domain.valueobject.ProductDetailInfo;
import com.cartethyia.easyorange.favorite.domain.valueobject.ProductInfo;
import com.cartethyia.easyorange.favorite.domain.valueobject.SellerInfo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ProductInfoPort {

    /** 查商品当前售价；不存在时返回 empty（调用方视为"商品不存在"）。 */
    Optional<BigDecimal> findPriceByProductId(String productId);

    boolean isOwnProduct(String userId, String productId);

    List<ProductInfo> findProductsByIds(List<String> productIds);

    Map<String, SellerInfo> findSellersByIds(Set<String> sellerIds);

    List<ProductDetailInfo> assembleProductDetails(List<ProductInfo> products, Map<String, SellerInfo> sellerMap);
}
