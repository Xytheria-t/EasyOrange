package com.cartethyia.easyorange.product.domain.port.output;

import com.cartethyia.easyorange.product.domain.valueobject.SellerInfo;

import java.util.Collection;
import java.util.Map;

public interface SellerInfoPort extends OutboundPort {

    SellerInfo getSellerInfo(Long sellerId);

    Map<Long, SellerInfo> getSellerInfos(Collection<Long> sellerIds);
}
