package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.domain.entity.ProductRating;
import java.util.Optional;

public interface ProductRatingRepository {

    Optional<ProductRating> findById(String id);

    void save(ProductRating rating);

    void update(ProductRating rating);

    void deleteById(String id);

    void incrementLikes(String id);

    /**
     * 该买家在此订单下是否已有评价（含已软删除的评价——DB 唯一键 {@code (user_id, order_id)} 不区分删除标记）。
     */
    boolean existsByUserIdAndOrderId(String userId, String orderId);
}
