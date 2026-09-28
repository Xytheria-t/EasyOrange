package com.cartethyia.easyorange.product.adapter.outbound.cache;

import com.cartethyia.easyorange.product.application.port.cache.CategoryCachePort;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 分类缓存适配器 — Spring Cache 注解式（纯 Redis 单层，见 framework {@code RedisCacheConfig}）。
 * <p>
 * 只缓存「未做商品计数富化」的分类列表（富化在 {@code CategoryQueryHandler} 完成），
 * 避免商品上下架触发分类缓存失效。Redis 故障由框架级 {@code CacheErrorHandler} fail-open，降级直查 DB。
 * <p>
 * 缓存的是**启用中**分类 —— 禁用即不出现，是「禁用」在读路径上的唯一语义落点。
 */
@Component
@RequiredArgsConstructor
public class CategoryCacheAdapter implements CategoryCachePort {

    private final CategoryQueryRepository categoryQueryRepository;

    @Override
    @Cacheable(
            cacheNames = ProductCacheConstant.CATEGORY_LIST_CACHE,
            key = "#parentId == null ? 'root' : #parentId")
    public List<CategoryReadModel> getCategoriesByParentId(String parentId) {
        return orEmpty(categoryQueryRepository.findEnabledByParentId(parentId));
    }

    @Override
    @CacheEvict(cacheNames = ProductCacheConstant.CATEGORY_LIST_CACHE, allEntries = true)
    public void evictAll() {
        // 失效由 @CacheEvict 代理执行，空实现仅满足端口契约
    }

    /**
     * 兜底空列表必须用可变 ArrayList：{@code List.of()} 是不可变 final 类（java.* 包），
     * 经 JSON 序列化器不带类型信息、无法反序列化，缓存会静默失效。
     */
    private static List<CategoryReadModel> orEmpty(List<CategoryReadModel> list) {
        return list != null ? list : new ArrayList<>();
    }
}
