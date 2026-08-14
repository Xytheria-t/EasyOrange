package com.cartethyia.easyorange.product.application.port.cache;

import com.cartethyia.easyorange.product.domain.valueobject.ViewCountEntry;
import java.util.Collection;
import java.util.List;

/**
 * 浏览量计数端口 — 浏览计数先落 Redis hash 缓冲，由 {@code ViewCountBatchProcessor} 定时批量落库。
 * 与 {@link ProductCachePort}（商品信息读缓存）职责分离。
 */
public interface ViewCountPort {

    /** 商品浏览量 +1（写入 Redis 缓冲，不直接落库）。 */
    void increment(String productId);

    /** 读取全部待落库的浏览计数。 */
    List<ViewCountEntry> findAllPending();

    /** 删除已落库的浏览计数（best-effort）。 */
    void removePending(Collection<String> productIds);
}
