package com.cartethyia.easyorange.product.application.port.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.domain.entity.ProductRating;

import java.util.List;
import java.util.Map;

public interface ProductRatingQueryRepository {

    PageResult<ProductRating> findByProductId(String productId, int pageNum, int pageSize);

    List<ProductRating> findAllByProductId(String productId);

    Map<Integer, Long> countByRatingGroup(String productId);
}
