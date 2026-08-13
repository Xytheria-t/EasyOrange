package com.cartethyia.easyorange.product.adapter.outbound.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.product.application.port.cache.CategoryCachePort;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * 分类缓存适配器测试 — 真实启用 Spring Cache AOP（ConcurrentMapCacheManager 替代 Redis），
 * 验证 {@code @Cacheable}/{@code @CacheEvict} 的命中/失效语义与 SpEL key 匹配。
 */
@SpringJUnitConfig(CategoryCacheAdapterTest.TestConfig.class)
@DisplayName("分类缓存适配器（Spring Cache 注解式）测试")
class CategoryCacheAdapterTest {

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        CategoryQueryRepository categoryQueryRepository() {
            return mock(CategoryQueryRepository.class);
        }

        @Bean
        CategoryCacheAdapter categoryCacheAdapter(CategoryQueryRepository repository) {
            return new CategoryCacheAdapter(repository);
        }
    }

    @Autowired
    private CacheManager cacheManager;

    // @EnableCaching 生成 JDK 动态代理，按接口注入
    @Autowired
    private CategoryCachePort adapter;

    @Autowired
    private CategoryQueryRepository repository;

    /**
     * Spring TestContext 跨方法复用同一 context：mock 调用计数累计 + 缓存数据泄漏。
     * 每个用例前 reset mock 并按具名缓存显式清空（动态缓存未创建前 {@code getCacheNames()} 为空集）。
     */
    @BeforeEach
    void setUp() {
        org.mockito.Mockito.reset(repository);
        var cache = cacheManager.getCache(ProductCacheConstant.CATEGORY_LIST_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    /** 必须用可变 ArrayList（List.of 不可反序列化），与生产 orEmpty 约定一致 */
    private static List<CategoryReadModel> listOf(CategoryReadModel... categories) {
        return new ArrayList<>(List.of(categories));
    }

    private static CategoryReadModel category(String id) {
        return new CategoryReadModel(id, "分类" + id, null, 1, null, 0, 1, null, 0);
    }

    @Nested
    @DisplayName("getCategoriesByLevel")
    class GetByLevelTests {

        @Test
        @DisplayName("缓存命中：二次读取不再查库")
        void cacheHit_returnsFromCache() {
            when(repository.findByLevel(1)).thenReturn(listOf(category("1")));

            adapter.getCategoriesByLevel(1);
            adapter.getCategoriesByLevel(1);

            verify(repository, times(1)).findByLevel(1);
        }

        @Test
        @DisplayName("evictByLevel 后重新查库")
        void evict_reloads() {
            when(repository.findByLevel(1)).thenReturn(listOf(category("1")));

            adapter.getCategoriesByLevel(1);
            adapter.evictByLevel(1);
            adapter.getCategoriesByLevel(1);

            verify(repository, times(2)).findByLevel(1);
        }

        @Test
        @DisplayName("DB 返回 null 时缓存空列表，不重复查库")
        void dbNull_cachesEmptyList() {
            when(repository.findByLevel(1)).thenReturn(null);

            assertThat(adapter.getCategoriesByLevel(1)).isEmpty();
            assertThat(adapter.getCategoriesByLevel(1)).isEmpty();

            verify(repository, times(1)).findByLevel(1);
        }

        @Test
        @DisplayName("level 为 null：跳过缓存直接查库")
        void nullLevel_skipsCache() {
            adapter.getCategoriesByLevel(null);

            verify(repository, times(1)).findByLevel(null);
        }
    }

    @Nested
    @DisplayName("getCategoriesByParentId")
    class GetByParentIdTests {

        @Test
        @DisplayName("缓存命中：二次读取不再查库")
        void cacheHit_returnsFromCache() {
            when(repository.findByParentId("1")).thenReturn(listOf(category("2")));

            adapter.getCategoriesByParentId("1");
            adapter.getCategoriesByParentId("1");

            verify(repository, times(1)).findByParentId("1");
        }

        @Test
        @DisplayName("evictByParentId 后重新查库")
        void evict_reloads() {
            when(repository.findByParentId("1")).thenReturn(listOf(category("2")));

            adapter.getCategoriesByParentId("1");
            adapter.evictByParentId("1");
            adapter.getCategoriesByParentId("1");

            verify(repository, times(2)).findByParentId("1");
        }

        @Test
        @DisplayName("parentId 为 null：跳过缓存直接查库")
        void nullParentId_skipsCache() {
            adapter.getCategoriesByParentId(null);

            verify(repository, times(1)).findByParentId(null);
        }

        @Test
        @DisplayName("不同 parentId 使用不同缓存 key，互不干扰")
        void differentParentIds_isolated() {
            when(repository.findByParentId("1")).thenReturn(listOf(category("a")));
            when(repository.findByParentId("2")).thenReturn(listOf(category("b")));

            adapter.getCategoriesByParentId("1");
            adapter.getCategoriesByParentId("2");
            adapter.getCategoriesByParentId("1");
            adapter.getCategoriesByParentId("2");

            verify(repository, times(1)).findByParentId("1");
            verify(repository, times(1)).findByParentId("2");
        }
    }

    @Nested
    @DisplayName("key 隔离")
    class KeyIsolationTests {

        @Test
        @DisplayName("level:1 的缓存不误伤 parent:1")
        void levelAndParentKeysDoNotCollide() {
            when(repository.findByLevel(1)).thenReturn(listOf(category("1")));
            when(repository.findByParentId("1")).thenReturn(listOf(category("2")));

            adapter.getCategoriesByLevel(1);
            adapter.evictByParentId("1");
            adapter.getCategoriesByLevel(1);

            // level:1 首次回源后缓存，parent:1 的失效不影响它 → findByLevel 只执行一次
            verify(repository, times(1)).findByLevel(1);
        }
    }
}
