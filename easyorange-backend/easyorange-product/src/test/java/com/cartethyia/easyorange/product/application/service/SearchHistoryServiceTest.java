package com.cartethyia.easyorange.product.application.service;

import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchHistoryService 单元测试")
class SearchHistoryServiceTest {

    @Mock
    private ProductQueryRepository productQueryRepository;

    @InjectMocks
    private SearchHistoryService searchHistoryService;

    @Nested
    @DisplayName("saveSearchHistory")
    class SaveSearchHistoryTests {

        @Test
        @DisplayName("保存搜索历史成功")
        void saveSearchHistory_success() {
            searchHistoryService.saveSearchHistory(1L, "手机");

            verify(productQueryRepository).saveSearchHistory(1L, "手机");
        }
    }

    @Nested
    @DisplayName("getSearchHistory")
    class GetSearchHistoryTests {

        @Test
        @DisplayName("获取搜索历史列表成功")
        void getSearchHistory_returnsList() {
            List<SearchHistoryReadModel> expected = List.of(
                    new SearchHistoryReadModel(1L, "手机", LocalDateTime.now())
            );
            when(productQueryRepository.findSearchHistoryByUserId(1L, 10)).thenReturn(expected);

            List<SearchHistoryReadModel> result = searchHistoryService.getSearchHistory(1L, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).keyword()).isEqualTo("手机");
        }

        @Test
        @DisplayName("搜索历史为空时返回空列表")
        void getSearchHistory_returnsEmptyList() {
            when(productQueryRepository.findSearchHistoryByUserId(1L, 10)).thenReturn(List.of());

            List<SearchHistoryReadModel> result = searchHistoryService.getSearchHistory(1L, 10);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("clearSearchHistory")
    class ClearSearchHistoryTests {

        @Test
        @DisplayName("清除搜索历史成功")
        void clearSearchHistory_success() {
            searchHistoryService.clearSearchHistory(1L);

            verify(productQueryRepository).clearSearchHistory(1L);
        }
    }

    @Nested
    @DisplayName("deleteSearchHistoryById")
    class DeleteSearchHistoryByIdTests {

        @Test
        @DisplayName("删除单条搜索历史成功")
        void deleteSearchHistoryById_success() {
            searchHistoryService.deleteSearchHistoryById(100L, 1L);

            verify(productQueryRepository).deleteSearchHistoryById(100L, 1L);
        }
    }
}
