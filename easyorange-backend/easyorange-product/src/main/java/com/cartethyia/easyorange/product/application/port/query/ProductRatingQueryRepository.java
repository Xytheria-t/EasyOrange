package com.cartethyia.easyorange.product.application.port.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.domain.entity.ProductRating;
import java.util.List;
import java.util.Map;

public interface ProductRatingQueryRepository {

    PageResult<ProductRating> findByProductId(String productId, int pageNum, int pageSize);

    List<ProductRating> findAllByProductId(String productId);

    Map<Integer, Long> countByRatingGroup(String productId);

    /**
     * 该用户在此订单下是否已有评价（含已软删除的评价——DB 唯一键 {@code (user_id, order_id)} 不区分删除标记）。
     */
    boolean existsByUserIdAndOrderId(String userId, String orderId);
}
