package com.cartethyia.easyorange.product.domain.port;

import com.cartethyia.easyorange.product.domain.valueobject.SellerInfo;
import java.util.Collection;
import java.util.Map;

public interface SellerInfoPort {

    Map<String, SellerInfo> getSellerInfos(Collection<String> sellerIds);
}
