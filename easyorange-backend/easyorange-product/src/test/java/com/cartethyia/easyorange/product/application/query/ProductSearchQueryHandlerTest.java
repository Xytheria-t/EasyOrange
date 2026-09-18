package com.cartethyia.easyorange.product.application.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.port.query.AiSearchEnhancerPort;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductSearchQueryPort;
import com.cartethyia.easyorange.product.application.port.query.QueryEmbeddingPort;
import com.cartethyia.easyorange.product.application.port.query.SearchResult;
import com.cartethyia.easyorange.product.application.query.dto.ProductSearchResult;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSearchQueryHandler 测试")
class ProductSearchQueryHandlerTest {

    @Mock
    private ProductQueryRepository productQueryRepository;

    @Mock
    private ProductSearchQueryPort searchQueryPort;

    @Mock
    private QueryEmbeddingPort queryEmbeddingPort;

    private ProductSearchQueryHandler searchQueryHandler;
    private ProductReadModel testProduct;

    @BeforeEach
    void setUp() {
        searchQueryHandler = handlerWith(null, null);

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
    @DisplayName("ES 检索路径未显式指定状态时默认只搜上架商品")
    void search_esPort_withoutStatus_shouldDefaultToOnline() {
        var handler = handlerWith(searchQueryPort, null);
        var criteria = new ProductSearchCriteria("手机", null, null, null, null, null, null, null, 1, 20);
        when(searchQueryPort.search(any())).thenAnswer(inv -> {
            var query = inv.getArgument(0, ProductSearchQueryPort.ProductSearchQuery.class);
            assertThat(query.status()).isEqualTo("ONLINE");
            return new SearchResult(List.of(testProduct), 1L, 1, 20, List.of(), List.of(), List.of());
        });

        var result = handler.search(criteria, false);

        assertThat(result.page().records()).hasSize(1);
    }

    @Test
    @DisplayName("ES 检索路径显式指定状态时按指定状态过滤")
    void search_esPort_withStatus_shouldPassThrough() {
        var handler = handlerWith(searchQueryPort, null);
        var criteria = new ProductSearchCriteria("手机", null, "OFFLINE", null, null, null, null, null, 1, 20);
        when(searchQueryPort.search(any())).thenAnswer(inv -> {
            var query = inv.getArgument(0, ProductSearchQueryPort.ProductSearchQuery.class);
            assertThat(query.status()).isEqualTo("OFFLINE");
            return new SearchResult(List.of(), 0L, 1, 20, List.of(), List.of(), List.of());
        });

        handler.search(criteria, false);

        verify(searchQueryPort).search(any());
    }

    @Test
    @DisplayName("获取搜索历史应返回历史列表")
    void getMySearchHistory_shouldReturnHistory() {
        SearchHistoryReadModel history = new SearchHistoryReadModel("100", "手机", LocalDateTime.now());
        when(productQueryRepository.findSearchHistoryByUserId("1", 10)).thenReturn(List.of(history));

        List<SearchHistoryReadModel> result = searchQueryHandler.getMySearchHistory("1", 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("100");
        assertThat(result.get(0).keyword()).isEqualTo("手机");
    }

    @Test
    @DisplayName("清除搜索历史应委托给 ProductQueryRepository")
    void clearMySearchHistory_shouldDelegate() {
        searchQueryHandler.clearMySearchHistory("1");

        verify(productQueryRepository).clearSearchHistory("1");
    }

    @Test
    @DisplayName("删除单条搜索历史应委托给 ProductQueryRepository")
    void deleteSearchHistory_shouldDelegate() {
        searchQueryHandler.deleteSearchHistory("1", "100");

        verify(productQueryRepository).deleteSearchHistoryById("100", "1");
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
        searchQueryHandler.recordSearch("1", "手机");

        verify(productQueryRepository).saveSearchHistory("1", "手机");
    }

    @Test
    @DisplayName("按相关性排序时向量化关键词，并把向量交给 ES 走两路召回")
    void search_relevanceSort_shouldEmbedKeywordAndEnableTwoLeg() {
        var handler = handlerWith(searchQueryPort, queryEmbeddingPort);
        when(queryEmbeddingPort.embed("手机")).thenReturn(List.of(0.1f, 0.2f));
        var criteria = new ProductSearchCriteria("手机", null, null, null, null, null, null, null, 1, 20);
        when(searchQueryPort.search(any())).thenAnswer(inv -> {
            var query = inv.getArgument(0, ProductSearchQueryPort.ProductSearchQuery.class);
            assertThat(query.useSemanticSearch()).isTrue();
            assertThat(query.queryEmbedding()).containsExactly(0.1f, 0.2f);
            return new SearchResult(List.of(testProduct), 1L, 1, 20, List.of(), List.of(), List.of());
        });

        var result = handler.search(criteria, false);

        assertThat(result.page().records()).hasSize(1);
        verify(queryEmbeddingPort).embed("手机");
    }

    @Test
    @DisplayName("显式排序（价格）不向量化：融合排名会和用户点选的排序打架")
    void search_explicitSort_shouldNotEmbed() {
        var handler = handlerWith(searchQueryPort, queryEmbeddingPort);
        var criteria = new ProductSearchCriteria("手机", null, null, null, null, null, "price_asc", null, 1, 20);
        when(searchQueryPort.search(any())).thenAnswer(inv -> {
            var query = inv.getArgument(0, ProductSearchQueryPort.ProductSearchQuery.class);
            assertThat(query.useSemanticSearch()).isFalse();
            assertThat(query.queryEmbedding()).isEmpty();
            return new SearchResult(List.of(testProduct), 1L, 1, 20, List.of(), List.of(), List.of());
        });

        handler.search(criteria, false);

        verifyNoInteractions(queryEmbeddingPort);
    }

    @Test
    @DisplayName("关键词为空不向量化：纯筛选浏览没有检索意图可编码")
    void search_blankKeyword_shouldNotEmbed() {
        var handler = handlerWith(searchQueryPort, queryEmbeddingPort);
        var criteria = new ProductSearchCriteria(null, "10", null, null, null, null, null, null, 1, 20);
        when(searchQueryPort.search(any()))
                .thenReturn(new SearchResult(List.of(testProduct), 1L, 1, 20, List.of(), List.of(), List.of()));

        handler.search(criteria, false);

        verifyNoInteractions(queryEmbeddingPort);
    }

    @Test
    @DisplayName("向量化端口缺失（无 AI key）时退化为单路召回，不抛异常")
    void search_withoutEmbeddingPort_shouldDegradeToSingleLeg() {
        var handler = handlerWith(searchQueryPort, null);
        var criteria = new ProductSearchCriteria("手机", null, null, null, null, null, null, null, 1, 20);
        when(searchQueryPort.search(any())).thenAnswer(inv -> {
            var query = inv.getArgument(0, ProductSearchQueryPort.ProductSearchQuery.class);
            assertThat(query.useSemanticSearch()).isFalse();
            return new SearchResult(List.of(testProduct), 1L, 1, 20, List.of(), List.of(), List.of());
        });

        var result = handler.search(criteria, false);

        assertThat(result.page().records()).hasSize(1);
    }

    /** 按需装配：两个可选出站端口谁在测试里被用到就传谁，传 null 即模拟该 bean 不存在。 */
    private ProductSearchQueryHandler handlerWith(ProductSearchQueryPort esPort, QueryEmbeddingPort embeddingPort) {
        return new ProductSearchQueryHandler(
                productQueryRepository,
                provider(esPort),
                provider((AiSearchEnhancerPort) null),
                provider(embeddingPort));
    }

    /** 构造最小 ObjectProvider：bean 为 null 时 getIfAvailable() 返回 null（模拟可选依赖缺失）。 */
    private static <T> ObjectProvider<T> provider(T bean) {
        return new ObjectProvider<T>() {
            @Override
            public T getIfAvailable() {
                return bean;
            }
        };
    }
}
