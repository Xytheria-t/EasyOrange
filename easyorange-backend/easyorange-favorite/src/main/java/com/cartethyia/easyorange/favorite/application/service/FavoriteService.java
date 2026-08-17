package com.cartethyia.easyorange.favorite.application.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import com.cartethyia.easyorange.favorite.domain.aggregate.FavoriteCreateSpec;
import com.cartethyia.easyorange.favorite.domain.port.PriceDropNotificationPort;
import com.cartethyia.easyorange.favorite.domain.port.ProductInfoPort;
import com.cartethyia.easyorange.favorite.domain.repository.FavoriteRepository;
import java.math.BigDecimal;
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
    private final PriceDropNotificationPort priceDropNotificationPort;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            ProductInfoPort productInfoPort,
            PriceDropNotificationPort priceDropNotificationPort) {
        this.favoriteRepository = favoriteRepository;
        this.productInfoPort = productInfoPort;
        this.priceDropNotificationPort = priceDropNotificationPort;
    }

    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(String userId, String productId) {
        BigDecimal price =
                productInfoPort.findPriceByProductId(productId).orElseThrow(() -> BusinessException.of("商品不存在"));
        BizRequire.requireTrue(!productInfoPort.isOwnProduct(userId, productId), "不能收藏自己的商品");
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }

        Favorite favorite = Favorite.create(new FavoriteCreateSpec(userId, productId, price));
        try {
            favoriteRepository.save(favorite);
        } catch (DuplicateKeyException e) {
            // 并发重复收藏：唯一键 (user_id, product_id, del_flag) 兜底，幂等视为成功
            log.warn("并发重复收藏被唯一键拦截, userId={}, productId={}", userId, productId);
        }
    }

    /**
     * 处理商品更新事件：对收藏了该商品的用户比对价格快照，降价则更新快照并通知。
     * <p>
     * 快照语义：初始为收藏时价格，通知后更新为最新价格——只提醒"再创新低"，避免重复噪音。
     * 幂等：快照更新走 CAS（{@code WHERE price_snapshot = 旧值}），重复事件到达时条件不命中，不重复通知。
     */
    @Transactional(rollbackFor = Exception.class)
    public void processPriceDrop(String productId, String productName, BigDecimal newPrice) {
        List<Favorite> favorites = favoriteRepository.findByProductId(productId);
        for (Favorite favorite : favorites) {
            BigDecimal snapshot = favorite.priceSnapshot();
            if (snapshot == null) {
                // 存量数据未回填快照：只回填不通知，避免迁移后首次事件误报降价
                favoriteRepository.updatePriceSnapshot(favorite.id(), null, newPrice);
                continue;
            }
            if (!favorite.isPriceDrop(newPrice)) {
                continue;
            }
            boolean updated = favoriteRepository.updatePriceSnapshot(favorite.id(), snapshot, newPrice);
            if (!updated) {
                // 并发场景下快照已被其他事件更新，由下一事件继续评估
                continue;
            }
            priceDropNotificationPort.sendPriceDropNotification(
                    favorite.userId(), productId, productName, snapshot, newPrice);
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
