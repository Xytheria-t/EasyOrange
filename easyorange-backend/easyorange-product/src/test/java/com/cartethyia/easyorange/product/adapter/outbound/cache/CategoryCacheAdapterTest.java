package com.cartethyia.easyorange.product.adapter.outbound.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryCacheAdapter 多级缓存适配器")
class CategoryCacheAdapterTest {

    @Mock
    private MultiLevelCache cache;

    @Mock
    private CategoryQueryRepository repository;

    private CategoryCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CategoryCacheAdapter(repository, cache);
    }

    private static CategoryReadModel category(String id) {
        return new CategoryReadModel(id, "分类" + id, null, 1, null, 0, 1, null, 0);
    }

    @Nested
    @DisplayName("getCategoriesByLevel")
    class GetByLevelTests {

        @Test
        @DisplayName("缓存命中时返回缓存列表，不查库")
        void cacheHit_returnsFromCache() {
            var expected = List.of(category("1"), category("2"));
            when(cache.get(eq("level:1"), eq(List.class), any())).thenReturn(expected);

            var result = adapter.getCategoriesByLevel(1);

            assertThat(result).isEqualTo(expected);
            verify(repository, never()).findByLevel(any());
        }

        @Test
        @DisplayName("缓存异常时降级直查 DB")
        void cacheFailure_fallsBackToDb() {
            var expected = List.of(category("9"));
            when(cache.get(eq("level:1"), eq(List.class), any())).thenThrow(new RuntimeException("Redis 不可用"));
            when(repository.findByLevel(1)).thenReturn(expected);

            var result = adapter.getCategoriesByLevel(1);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("DB 返回 null 时返回空列表")
        void dbNull_returnsEmptyList() {
            when(cache.get(eq("level:1"), eq(List.class), any())).thenThrow(new RuntimeException("Redis 不可用"));
            when(repository.findByLevel(1)).thenReturn(null);

            assertThat(adapter.getCategoriesByLevel(1)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCategoriesByParentId")
    class GetByParentTests {

        @Test
        @DisplayName("以 parent:{id} 为键查缓存")
        void usesParentKey() {
            var expected = List.of(category("5"));
            when(cache.get(eq("parent:abc"), eq(List.class), any())).thenReturn(expected);

            assertThat(adapter.getCategoriesByParentId("abc")).isEqualTo(expected);
        }

        @Test
        @DisplayName("缓存异常时降级直查 DB")
        void cacheFailure_fallsBackToDb() {
            var expected = List.of(category("5"));
            when(cache.get(eq("parent:abc"), eq(List.class), any())).thenThrow(new RuntimeException("Redis 不可用"));
            when(repository.findByParentId("abc")).thenReturn(expected);

            assertThat(adapter.getCategoriesByParentId("abc")).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("getCategoryById")
    class GetByIdTests {

        @Test
        @DisplayName("null id 直接返回 empty，不触缓存")
        void nullId_returnsEmpty() {
            assertThat(adapter.getCategoryById(null)).isEmpty();
            verify(cache, never()).get(any(), any(), any());
        }

        @Test
        @DisplayName("以 id:{id} 为键查缓存，命中返回 Optional")
        void cacheHit_returnsOptional() {
            var expected = category("7");
            when(cache.get(eq("id:7"), eq(CategoryReadModel.class), any())).thenReturn(expected);

            assertThat(adapter.getCategoryById("7")).contains(expected);
        }

        @Test
        @DisplayName("缓存未命中返回 empty")
        void cacheMiss_returnsEmpty() {
            when(cache.get(eq("id:7"), eq(CategoryReadModel.class), any())).thenReturn(null);

            assertThat(adapter.getCategoryById("7")).isEmpty();
        }
    }

    @Nested
    @DisplayName("失效")
    class EvictTests {

        @Test
        @DisplayName("evictAll 委托 cache.clear()")
        void evictAll_clearsCache() {
            adapter.evictAll();
            verify(cache).clear();
        }

        @Test
        @DisplayName("evictByLevel 以 level:{level} 为键失效")
        void evictByLevel_evictsKey() {
            adapter.evictByLevel(2);
            verify(cache).evict("level:2");
        }

        @Test
        @DisplayName("evictByParentId 以 parent:{id} 为键失效")
        void evictByParentId_evictsKey() {
            adapter.evictByParentId("xyz");
            verify(cache).evict("parent:xyz");
        }
    }
}
