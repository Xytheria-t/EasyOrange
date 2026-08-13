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
 * 缓存的是「未做商品计数富化」的原始分类列表（富化在 {@code CategoryQueryHandler} 完成），
 * 避免计数变化触发缓存失效风暴。写路径失效由 admin 侧 {@code evictByLevel}/{@code evictByParentId}
 * 显式触发。Redis 故障由框架级 {@code CacheErrorHandler} fail-open，降级直查 DB。
 */
@Component
@RequiredArgsConstructor
public class CategoryCacheAdapter implements CategoryCachePort {

    private final CategoryQueryRepository categoryQueryRepository;

    @Override
    @Cacheable(
            cacheNames = ProductCacheConstant.CATEGORY_LIST_CACHE,
            key = "'level:' + #level",
            condition = "#level != null")
    public List<CategoryReadModel> getCategoriesByLevel(Integer level) {
        return orEmpty(categoryQueryRepository.findByLevel(level));
    }

    @Override
    @Cacheable(
            cacheNames = ProductCacheConstant.CATEGORY_LIST_CACHE,
            key = "'parent:' + #parentId",
            condition = "#parentId != null")
    public List<CategoryReadModel> getCategoriesByParentId(String parentId) {
        return orEmpty(categoryQueryRepository.findByParentId(parentId));
    }

    @Override
    @CacheEvict(
            cacheNames = ProductCacheConstant.CATEGORY_LIST_CACHE,
            key = "'level:' + #level",
            condition = "#level != null")
    public void evictByLevel(Integer level) {
        // 失效由 @CacheEvict 代理执行，空实现仅满足端口契约
    }

    @Override
    @CacheEvict(
            cacheNames = ProductCacheConstant.CATEGORY_LIST_CACHE,
            key = "'parent:' + #parentId",
            condition = "#parentId != null")
    public void evictByParentId(String parentId) {
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
