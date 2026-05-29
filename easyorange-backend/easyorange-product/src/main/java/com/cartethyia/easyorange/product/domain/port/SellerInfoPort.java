package com.cartethyia.easyorange.product.domain.port;

import com.cartethyia.easyorange.product.domain.valueobject.SellerInfo;

import java.util.Collection;
import java.util.Map;

public interface SellerInfoPort {

    SellerInfo getSellerInfo(Long sellerId);

    Map<Long, SellerInfo> getSellerInfos(Collection<Long> sellerIds);
}