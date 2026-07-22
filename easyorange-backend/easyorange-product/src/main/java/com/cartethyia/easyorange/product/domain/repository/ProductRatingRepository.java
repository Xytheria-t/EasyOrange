package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.domain.entity.ProductRating;

import java.util.Optional;

public interface ProductRatingRepository {

    Optional<ProductRating> findById(String id);

    void save(ProductRating rating);

    void update(ProductRating rating);

    void deleteById(String id);

    void incrementLikes(String id);
}
