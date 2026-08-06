package com.cartethyia.easyorange.product.application.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.query.dto.ProductSearchResult;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSearchQueryHandler 测试")
class ProductSearchQueryHandlerTest {

    @Mock
    private ProductQueryRepository productQueryRepository;

    private ProductSearchQueryHandler searchQueryHandler;
    private ProductReadModel testProduct;

    @BeforeEach
    void setUp() {
        searchQueryHandler = new ProductSearchQueryHandler(productQueryRepository, Optional.empty(), Optional.empty());

        testProduct = new ProductReadModel(
                "1",
                "10",
                "资产方",
                null,
                "2",
                "分类",
                "测试商品",
                "描述",
                new BigDecimal("100"),
                null,
                10,
                "1",
                "上架",
                0,
                "1",
                "全新",
                "北京",
                "微信",
                List.of("http://img/1.jpg"),
                "http://img/1.jpg",
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    @Test
    @DisplayName("搜索商品应返回分页结果")
    void search_shouldReturnPageResult() {
        var criteria = new ProductSearchCriteria("手机", "2", "1", null, null, null, null, null, 1, 20);
        PageResult<ProductReadModel> page = PageResult.of(List.of(testProduct), 1, 1, 20);
        when(productQueryRepository.searchProducts(criteria)).thenReturn(page);

        ProductSearchResult result = searchQueryHandler.search(criteria, false);

        assertThat(result).isNotNull();
        assertThat(result.page().records()).hasSize(1);
        assertThat(result.page().total()).isEqualTo(1);
        assertThat(result.page().records().get(0).id()).isEqualTo("1");
        assertThat(result.page().records().get(0).title()).isEqualTo("测试商品");
    }

    @Test
    @DisplayName("搜索商品无结果应返回空分页")
    void search_withNoResults_shouldReturnEmptyPage() {
        var criteria = new ProductSearchCriteria("不存在", null, null, null, null, null, null, null, 1, 20);
        PageResult<ProductReadModel> page = PageResult.of(List.of(), 0, 1, 20);
        when(productQueryRepository.searchProducts(criteria)).thenReturn(page);

        ProductSearchResult result = searchQueryHandler.search(criteria, false);

        assertThat(result.page().records()).isEmpty();
        assertThat(result.page().total()).isZero();
    }

    @Test
    @DisplayName("搜索使用默认分页参数")
    void search_withNullPageParams_shouldUseDefaults() {
        var criteria = new ProductSearchCriteria("手机", null, null, null, null, null, null, null, null, null);
        PageResult<ProductReadModel> page = PageResult.of(List.of(), 0, 1, 20);
        when(productQueryRepository.searchProducts(any())).thenReturn(page);

        searchQueryHandler.search(criteria, false);

        verify(productQueryRepository).searchProducts(any());
    }

    @Test
    @DisplayName("获取搜索历史应返回历史列表")
    void getMySearchHistory_shouldReturnHistory() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            SearchHistoryReadModel history = new SearchHistoryReadModel("100", "手机", LocalDateTime.now());
            when(productQueryRepository.findSearchHistoryByUserId("1", 10)).thenReturn(List.of(history));

            List<SearchHistoryReadModel> result = searchQueryHandler.getMySearchHistory(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo("100");
            assertThat(result.get(0).keyword()).isEqualTo("手机");
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("清除搜索历史应委托给 ProductQueryRepository")
    void clearMySearchHistory_shouldDelegate() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            searchQueryHandler.clearMySearchHistory();

            verify(productQueryRepository).clearSearchHistory("1");
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("删除单条搜索历史应委托给 ProductQueryRepository")
    void deleteSearchHistory_shouldDelegate() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            searchQueryHandler.deleteSearchHistory("100");

            verify(productQueryRepository).deleteSearchHistoryById("100", "1");
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("获取热门关键词应返回列表")
    void getHotKeywords_shouldReturnKeywords() {
        HotKeywordReadModel keyword = new HotKeywordReadModel("1", "手机", 100, 5);
        when(productQueryRepository.findHotKeywords(10)).thenReturn(List.of(keyword));

        List<HotKeywordReadModel> result = searchQueryHandler.getHotKeywords(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("1");
        assertThat(result.get(0).keyword()).isEqualTo("手机");
        assertThat(result.get(0).searchCount()).isEqualTo(100);
        assertThat(result.get(0).hotLevel()).isEqualTo(5);
    }

    @Test
    @DisplayName("获取搜索建议应返回建议列表")
    void getSearchSuggestions_shouldReturnSuggestions() {
        when(productQueryRepository.findSearchSuggestions("手", 10)).thenReturn(List.of("手机", "手表", "手套"));

        List<String> result = searchQueryHandler.getSearchSuggestions("手", 10);

        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("手机", "手表", "手套");
    }

    @Test
    @DisplayName("记录搜索应委托给 ProductQueryRepository")
    void recordSearch_shouldDelegate() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            searchQueryHandler.recordSearch("手机");

            verify(productQueryRepository).saveSearchHistory("1", "手机");
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }
}
