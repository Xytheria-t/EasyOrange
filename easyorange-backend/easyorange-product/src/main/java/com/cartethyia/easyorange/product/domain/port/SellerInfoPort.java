package com.cartethyia.easyorange.product.domain.port;

import com.cartethyia.easyorange.product.domain.valueobject.SellerInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface SellerInfoPort {

    Optional<SellerInfo> getSellerInfo(String sellerId);

    Map<String, SellerInfo> getSellerInfos(Collection<String> sellerIds);
}