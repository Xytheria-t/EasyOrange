package com.cartethyia.easyorange.product.application.port.cache;

import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import java.util.Map;
import java.util.Set;

public interface SellerCachePort {

    Map<String, SellerReadModel> getSellers(Set<String> sellerIds);
}
