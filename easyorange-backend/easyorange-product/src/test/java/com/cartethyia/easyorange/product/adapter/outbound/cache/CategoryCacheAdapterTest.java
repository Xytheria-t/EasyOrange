package com.cartethyia.easyorange.product.adapter.outbound.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.product.application.port.cache.CategoryCachePort;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import com.cartethyia.easyorange.product.domain.enums.CategoryStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
 * <p>
 * 覆盖点从「按 level/parent 逐 key 失效」换成「整份失效」：旧实现要枚举所有受影响的 key，
 * 漏一个不报错、只是分类列表悄悄不对；新的性质是 {@code evictAll} 之后一定重新查库。
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

    /** 按接口注入：@Cacheable/@CacheEvict 让容器里的 bean 是代理，按实现类注入会类型不符 */
    @Autowired
    private CategoryCachePort adapter;

    @Autowired
    private CategoryQueryRepository repository;

    @Autowired
    private CacheManager cacheManager;

    /**
     * 每个用例前清缓存并重置 mock。
     * <p>
     * {@code @SpringJUnitConfig} 的上下文是跨用例复用的：不清理的话上一个用例留下的缓存条目
     * 会让本用例的「二次读取」直接命中、查库次数对不上（而这恰恰是本测试要断言的东西）。
     */
    @BeforeEach
    void resetState() {
        Mockito.reset(repository);
        org.springframework.cache.Cache cache = cacheManager.getCache(ProductCacheConstant.CATEGORY_LIST_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Nested
    @DisplayName("getCategoriesByParentId")
    class GetByParentTests {

        @Test
        @DisplayName("缓存命中：二次读取不再查库")
        void cacheHit_returnsFromCache() {
            when(repository.findEnabledByParentId(null)).thenReturn(listOf(category("1")));

            adapter.getCategoriesByParentId(null);
            adapter.getCategoriesByParentId(null);

            verify(repository, times(1)).findEnabledByParentId(null);
        }

        @Test
        @DisplayName("不同父分类互不干扰")
        void differentParents_cachedSeparately() {
            when(repository.findEnabledByParentId("p1")).thenReturn(listOf(category("1")));
            when(repository.findEnabledByParentId("p2")).thenReturn(listOf(category("2")));

            assertThat(adapter.getCategoriesByParentId("p1"))
                    .extracting(CategoryReadModel::id)
                    .containsExactly("1");
            assertThat(adapter.getCategoriesByParentId("p2"))
                    .extracting(CategoryReadModel::id)
                    .containsExactly("2");

            verify(repository, times(1)).findEnabledByParentId("p1");
            verify(repository, times(1)).findEnabledByParentId("p2");
        }

        @Test
        @DisplayName("evictAll 后重新查库")
        void evictAll_reloads() {
            when(repository.findEnabledByParentId(null)).thenReturn(listOf(category("1")));

            adapter.getCategoriesByParentId(null);
            adapter.evictAll();
            adapter.getCategoriesByParentId(null);

            verify(repository, times(2)).findEnabledByParentId(null);
        }

        @Test
        @DisplayName("evictAll 清掉所有父分类的缓存（不漏 key）")
        void evictAll_clearsEveryParent() {
            when(repository.findEnabledByParentId("p1")).thenReturn(listOf(category("1")));
            when(repository.findEnabledByParentId("p2")).thenReturn(listOf(category("2")));

            adapter.getCategoriesByParentId("p1");
            adapter.getCategoriesByParentId("p2");
            adapter.evictAll();
            adapter.getCategoriesByParentId("p1");
            adapter.getCategoriesByParentId("p2");

            verify(repository, times(2)).findEnabledByParentId("p1");
            verify(repository, times(2)).findEnabledByParentId("p2");
        }
    }

    @Test
    @DisplayName("查库返回 null 时兜底为空列表（缓存反序列化需要可变 List）")
    void nullResult_fallsBackToEmptyList() {
        when(repository.findEnabledByParentId("p1")).thenReturn(null);

        var result = adapter.getCategoriesByParentId("p1");

        assertThat(result).isNotNull().isEmpty();
    }

    /** 必须用可变 ArrayList（List.of 不可反序列化），与生产 orEmpty 约定一致 */
    private static List<CategoryReadModel> listOf(CategoryReadModel... categories) {
        return new ArrayList<>(Arrays.asList(categories));
    }

    private static CategoryReadModel category(String id) {
        return new CategoryReadModel(id, "分类" + id, null, 1, null, 0, CategoryStatus.ENABLED, LocalDateTime.now(), 0);
    }
}
