package com.cartethyia.easyorange.favorite.domain.repository;

import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository {

    Optional<Favorite> findById(Long id);

    List<Favorite> findByIds(List<Long> ids);

    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);

    List<Favorite> findByUserId(Long userId, long offset, long limit);

    long countByUserId(Long userId);

    Favorite save(Favorite favorite);

    void removeById(Long id);

    int removeByIds(List<Long> ids);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
