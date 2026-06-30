package com.cartethyia.easyorange.favorite.domain.repository;

import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FavoriteRepository {

    Optional<Favorite> findById(String id);

    List<Favorite> findByIds(List<String> ids);

    Optional<Favorite> findByUserIdAndProductId(String userId, String productId);

    List<Favorite> findByUserId(String userId, long offset, long limit);

    long countByUserId(String userId);

    Favorite save(Favorite favorite);

    void removeById(String id);

    int removeByIds(List<String> ids);

    boolean existsByUserIdAndProductId(String userId, String productId);

    Set<String> findFavoritedProductIds(String userId, List<String> productIds);
}
