package com.cartethyia.easyorange.favorite.domain.repository;

import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FavoriteRepository {

    List<Favorite> findByIds(List<String> ids);

    Optional<Favorite> findByUserIdAndProductId(String userId, String productId);

    List<Favorite> findByUserId(String userId, long offset, long limit);

    /** 查某商品的全部活跃收藏（降价提醒用，含软删位过滤）。 */
    List<Favorite> findByProductId(String productId);

    long countByUserId(String userId);

    Favorite save(Favorite favorite);

    void removeById(String id);

    int removeByIds(List<String> ids);

    boolean existsByUserIdAndProductId(String userId, String productId);

    Set<String> findFavoritedProductIds(String userId, List<String> productIds);

    /**
     * CAS 更新价格快照：仅当当前快照等于 expectedSnapshot 时更新为新值，返回是否命中。
     * <p>
     * expectedSnapshot 为 null 时匹配快照为空的存量行（回填语义）。幂等：重复事件到达时快照已更新，
     * 条件不命中返回 false，天然去重。
     */
    boolean updatePriceSnapshot(String id, BigDecimal expectedSnapshot, BigDecimal newSnapshot);
}
