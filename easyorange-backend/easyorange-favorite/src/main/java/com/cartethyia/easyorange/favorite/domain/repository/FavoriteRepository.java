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

    /**
     * 幂等建立收藏：软删行复活、无行则新增；已被并发请求抢先建立（唯一键
     * {@code (user_id, product_id, del_flag)} 命中）时返回空，由调用方按幂等成功处理。
     * <p>
     * 唯一键冲突在适配器内翻译成"返回空"，不外抛持久层异常。
     */
    Optional<Favorite> saveIfAbsent(Favorite favorite);

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
