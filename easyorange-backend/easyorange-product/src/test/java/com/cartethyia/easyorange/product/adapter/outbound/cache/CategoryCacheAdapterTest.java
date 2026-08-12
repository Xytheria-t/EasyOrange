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
}
