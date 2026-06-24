package com.cartethyia.easyorange.product.application.query.handler;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.FacetBucketResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.SearchPageResponse;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductSearchRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.HotKeywordResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.SearchHistoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSearchHandler 测试")
class ProductSearchHandlerTest {

    @Mock
    private ProductQueryRepository productQueryRepository;

    private ProductSearchHandler searchHandler;
    private ProductReadModel testProduct;

    @BeforeEach
    void setUp() {
        searchHandler = new ProductSearchHandler(productQueryRepository, Optional.empty(), Optional.empty());

        testProduct = new ProductReadModel(
                1L, 10L, "卖家", null, 2L, "分类",
                "测试商品", "描述", new BigDecimal("100"), null,
                10, 1, "上架", 0, 1, "全新",
                "北京", "微信", List.of("http://img/1.jpg"),
                "http://img/1.jpg",
                null, 0, null, 0,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("搜索商品应返回分页结果")
    void handleSearch_shouldReturnPageResult() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword("手机");
        request.setCategoryId(2L);
        request.setStatus(1);

        PageResult<ProductReadModel> page = PageResult.of(List.of(testProduct), 1, 1, 20);
        when(productQueryRepository.searchProducts("手机", 2L, 1, 1, 20))
                .thenReturn(page);

        SearchPageResponse<ProductResponse> result = searchHandler.handleSearch(request);

        assertThat(result).isNotNull();
        assertThat(result.records()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records().get(0).getId()).isEqualTo(1L);
        assertThat(result.records().get(0).getTitle()).isEqualTo("测试商品");
        assertThat(result.records().get(0).getPrice()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(result.records().get(0).getMainImageUrl()).isEqualTo("http://img/1.jpg");
    }

    @Test
    @DisplayName("搜索商品无结果应返回空分页")
    void handleSearch_withNoResults_shouldReturnEmptyPage() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword("不存在");

        PageResult<ProductReadModel> page = PageResult.of(List.of(), 0, 1, 20);
        when(productQueryRepository.searchProducts("不存在", null, null, 1, 20))
                .thenReturn(page);

        SearchPageResponse<ProductResponse> result = searchHandler.handleSearch(request);
        assertThat(result.records()).isEmpty();
        assertThat(result.total()).isZero();
    }

    @Test
    @DisplayName("搜索使用默认分页参数")
    void handleSearch_withNullPageParams_shouldUseDefaults() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword("手机");

        PageResult<ProductReadModel> page = PageResult.of(List.of(), 0, 1, 20);
        when(productQueryRepository.searchProducts("手机", null, null, 1, 20))
                .thenReturn(page);

        searchHandler.handleSearch(request);

        verify(productQueryRepository).searchProducts("手机", null, null, 1, 20);
    }

    @Test
    @DisplayName("获取搜索历史应返回历史列表")
    void getMySearchHistory_shouldReturnHistory() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            SearchHistoryReadModel history = new SearchHistoryReadModel(100L, "手机", LocalDateTime.now());
            when(productQueryRepository.findSearchHistoryByUserId(1L, 10))
                    .thenReturn(List.of(history));

            List<SearchHistoryResponse> result = searchHandler.getMySearchHistory(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(100L);
            assertThat(result.get(0).getKeyword()).isEqualTo("手机");
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("清除搜索历史应委托给 ProductQueryRepository")
    void clearMySearchHistory_shouldDelegate() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            searchHandler.clearMySearchHistory();

            verify(productQueryRepository).clearSearchHistory(1L);
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("删除单条搜索历史应委托给 ProductQueryRepository")
    void deleteSearchHistory_shouldDelegate() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            searchHandler.deleteSearchHistory(100L);

            verify(productQueryRepository).deleteSearchHistoryById(100L, 1L);
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("获取热门关键词应返回列表")
    void getHotKeywords_shouldReturnKeywords() {
        HotKeywordReadModel keyword = new HotKeywordReadModel(1L, "手机", 100, 5);
        when(productQueryRepository.findHotKeywords(10)).thenReturn(List.of(keyword));

        List<HotKeywordResponse> result = searchHandler.getHotKeywords(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getKeyword()).isEqualTo("手机");
        assertThat(result.get(0).getSearchCount()).isEqualTo(100);
        assertThat(result.get(0).getHotLevel()).isEqualTo(5);
    }

    @Test
    @DisplayName("获取搜索建议应返回建议列表")
    void getSearchSuggestions_shouldReturnSuggestions() {
        when(productQueryRepository.findSearchSuggestions("手", 10))
                .thenReturn(List.of("手机", "手表", "手套"));

        List<String> result = searchHandler.getSearchSuggestions("手", 10);

        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("手机", "手表", "手套");
    }

    @Test
    @DisplayName("记录搜索应委托给 ProductQueryRepository")
    void recordSearch_shouldDelegate() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            searchHandler.recordSearch("手机");

            verify(productQueryRepository).saveSearchHistory(1L, "手机");
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }
}
