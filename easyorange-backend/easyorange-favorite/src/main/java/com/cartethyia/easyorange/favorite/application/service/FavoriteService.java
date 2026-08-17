package com.cartethyia.easyorange.favorite.application.service;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import com.cartethyia.easyorange.favorite.domain.aggregate.FavoriteCreateSpec;
import com.cartethyia.easyorange.favorite.domain.port.ProductInfoPort;
import com.cartethyia.easyorange.favorite.domain.repository.FavoriteRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductInfoPort productInfoPort;

    public FavoriteService(FavoriteRepository favoriteRepository, ProductInfoPort productInfoPort) {
        this.favoriteRepository = favoriteRepository;
        this.productInfoPort = productInfoPort;
    }

    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(String userId, String productId) {
        BizRequire.requireTrue(productInfoPort.productExists(productId), "商品不存在");
        BizRequire.requireTrue(!productInfoPort.isOwnProduct(userId, productId), "不能收藏自己的商品");
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }

        Favorite favorite = Favorite.create(new FavoriteCreateSpec(userId, productId));
        try {
            favoriteRepository.save(favorite);
        } catch (DuplicateKeyException e) {
            // 并发重复收藏：唯一键 (user_id, product_id, del_flag) 兜底，幂等视为成功
            log.warn("并发重复收藏被唯一键拦截, userId={}, productId={}", userId, productId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(String userId, String productId) {
        Favorite favorite =
                favoriteRepository.findByUserIdAndProductId(userId, productId).orElse(null);
        if (favorite == null) {
            return;
        }

        favorite.validateOwnership(userId);
        favoriteRepository.removeById(favorite.id());
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeManyFavorites(String userId, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Favorite> favorites = favoriteRepository.findByIds(ids);
        favorites.forEach(favorite -> favorite.validateOwnership(userId));

        favoriteRepository.removeByIds(ids);
    }

    @Transactional(readOnly = true)
    public PageResult<Favorite> queryFavorites(String userId, int pageNum, int pageSize) {
        long offset = (pageNum - 1L) * pageSize;
        long total = favoriteRepository.countByUserId(userId);
        List<Favorite> favorites = favoriteRepository.findByUserId(userId, offset, pageSize);

        return PageResult.of(favorites, total, pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(String userId, String productId) {
        return favoriteRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional(readOnly = true)
    public long getFavoriteCount(String userId) {
        return favoriteRepository.countByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> batchCheckFavorited(String userId, List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        Set<String> favoritedIds = favoriteRepository.findFavoritedProductIds(userId, productIds);
        return productIds.stream()
                .collect(Collectors.toMap(pid -> pid, favoritedIds::contains, (a, b) -> a, LinkedHashMap::new));
    }
}
