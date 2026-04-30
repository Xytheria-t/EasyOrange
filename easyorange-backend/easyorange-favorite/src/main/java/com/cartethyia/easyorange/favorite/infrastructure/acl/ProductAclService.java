package com.cartethyia.easyorange.favorite.infrastructure.acl;

import com.cartethyia.easyorange.product.application.query.dto.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.application.query.dto.SellerReadModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ProductAclService {

    boolean productExists(Long productId);

    boolean isOwnProduct(Long userId, Long productId);

    List<ProductReadModel> findProductsByIds(List<Long> productIds);

    Map<Long, SellerReadModel> findSellersByIds(Set<Long> sellerIds);

    List<ProductVO> assembleProductVOs(List<ProductReadModel> products, Map<Long, SellerReadModel> sellerMap);
}
