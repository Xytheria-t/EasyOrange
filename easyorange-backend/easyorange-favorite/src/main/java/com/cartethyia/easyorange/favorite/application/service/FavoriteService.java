package com.cartethyia.easyorange.favorite.application.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import com.cartethyia.easyorange.favorite.domain.port.ProductInfoPort;
import com.cartethyia.easyorange.favorite.domain.repository.FavoriteRepository;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductInfoPort productInfoPort;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           ProductInfoPort productInfoPort) {
        this.favoriteRepository = favoriteRepository;
        this.productInfoPort = productInfoPort;
    }

    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(Long productId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        BizRequire.requireTrue(productInfoPort.productExists(productId), "商品不存在");
        BizRequire.requireFalse(productInfoPort.isOwnProduct(userId, productId), "不能收藏自己的商品");
        BizRequire.requireFalse(
                favoriteRepository.existsByUserIdAndProductId(userId, productId),
                "已收藏过该商品"
        );

        Favorite favorite = Favorite.create(userId, productId);
        favoriteRepository.save(favorite);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long productId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Favorite favorite = favoriteRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> BusinessException.of("未收藏过该商品"));

        favorite.validateOwnership(userId);
        favoriteRepository.removeById(favorite.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeManyFavorites(List<Long> ids) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Favorite> favorites = favoriteRepository.findByIds(ids);
        favorites.forEach(favorite -> favorite.validateOwnership(userId));

        favoriteRepository.removeByIds(ids);
    }

    @Transactional(readOnly = true)
    public PageResult<Favorite> queryFavorites(int pageNum, int pageSize) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        long offset = (pageNum - 1L) * pageSize;
        long total = favoriteRepository.countByUserId(userId);
        List<Favorite> favorites = favoriteRepository.findByUserId(userId, offset, pageSize);

        return PageResult.of(favorites, total, pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(Long productId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return favoriteRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional(readOnly = true)
    public long getFavoriteCount() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return favoriteRepository.countByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> batchCheckFavorited(List<Long> productIds) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> favoritedIds = favoriteRepository.findFavoritedProductIds(userId, productIds);
        return productIds.stream()
                .collect(Collectors.toMap(pid -> pid, favoritedIds::contains, (a, b) -> a, LinkedHashMap::new));
    }
}
