package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.application.port.query.ProductSearchQueryPort.ProductSearchQuery;
import com.cartethyia.easyorange.product.application.port.query.SearchResult;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ElasticsearchProductSearchQueryAdapterTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ElasticsearchProductSearchQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ElasticsearchProductSearchQueryAdapter(elasticsearchOperations, objectMapper);
    }

    @Test
    @DisplayName("无搜索结果时应返回空")
    void search_shouldReturnEmptyResultWhenNoResults() {
        SearchHits<ProductDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);
        when(searchHits.getAggregations()).thenReturn(null);
        when(elasticsearchOperations.search(any(StringQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        ProductSearchQuery query = new ProductSearchQuery("test", null, null, null, null, null, null, 1, 20, null, false);
        SearchResult result = adapter.search(query);

        assertThat(result.total()).isZero();
        assertThat(result.records()).isEmpty();
        assertThat(result.categoryFacets()).isEmpty();
        assertThat(result.conditionFacets()).isEmpty();
        assertThat(result.priceRangeFacets()).isEmpty();
    }

    @Test
    @DisplayName("应正确映射搜索结果")
    void search_shouldReturnMappedResults() {
        ProductDocument doc = ProductDocument.builder()
                .id("100")
                .userId("200")
                .name("测试商品")
                .description("商品描述")
                .categoryId("300")
                .categoryName("手机")
                .price(99.99)
                .originalPrice(199.99)
                .conditionLevel("5")
                .status(ProductStatus.ONLINE.getCode())
                .viewCount(1000)
                .stock(10)
                .location("北京")
                .tags(List.of("tag1", "tag2"))
                .mainImage("http://example.com/main.jpg")
                .images(List.of("http://example.com/main.jpg"))
                .createTime(LocalDateTime.of(2025, 1, 1, 0, 0))
                .updateTime(LocalDateTime.of(2025, 1, 2, 0, 0))
                .build();

        SearchHit<ProductDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);

        SearchHits<ProductDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of(hit));
        when(searchHits.getTotalHits()).thenReturn(1L);
        when(searchHits.getAggregations()).thenReturn(null);

        when(elasticsearchOperations.search(any(StringQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        ProductSearchQuery query = new ProductSearchQuery("test", null, null, null, null, null, null, 1, 20, null, false);
        SearchResult result = adapter.search(query);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).hasSize(1);

        var record = result.records().get(0);
        assertThat(record.id()).isEqualTo("100");
        assertThat(record.sellerId()).isEqualTo("200");
        assertThat(record.title()).isEqualTo("测试商品");
        assertThat(record.description()).isEqualTo("商品描述");
        assertThat(record.categoryName()).isEqualTo("手机");
        assertThat(record.price()).isEqualByComparingTo("99.99");
        assertThat(record.originalPrice()).isEqualByComparingTo("199.99");
        assertThat(record.condition()).isEqualTo("5");
        assertThat(record.status()).isEqualTo(ProductStatus.ONLINE.getCode());
        assertThat(record.views()).isEqualTo(1000);
        assertThat(record.stock()).isEqualTo(10);
        assertThat(record.location()).isEqualTo("北京");
        assertThat(record.mainImageUrl()).isEqualTo("http://example.com/main.jpg");
        assertThat(record.images()).containsExactly("http://example.com/main.jpg");
        assertThat(result.categoryFacets()).isEmpty();
        assertThat(result.conditionFacets()).isEmpty();
        assertThat(result.priceRangeFacets()).isEmpty();
    }

    @Test
    @DisplayName("搜索查询应包含分类过滤条件")
    void search_shouldHandleCategoryFilter() {
        SearchHits<ProductDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);
        when(searchHits.getAggregations()).thenReturn(null);

        ArgumentCaptor<StringQuery> queryCaptor = ArgumentCaptor.forClass(StringQuery.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        ProductSearchQuery query = new ProductSearchQuery("手机", "300", null, null, null, null, null, 1, 20, null, false);
        adapter.search(query);

        StringQuery capturedQuery = queryCaptor.getValue();
        String json = capturedQuery.getSource();
        assertThat(json).contains("categoryId");
        assertThat(json).contains("300");
    }

    @Test
    @DisplayName("空关键词应执行 match_all 查询")
    void search_shouldUseMatchAllForBlankKeyword() {
        SearchHits<ProductDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);
        when(searchHits.getAggregations()).thenReturn(null);

        ArgumentCaptor<StringQuery> queryCaptor = ArgumentCaptor.forClass(StringQuery.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        ProductSearchQuery query = new ProductSearchQuery(null, null, null, null, null, null, null, 1, 20, null, false);
        adapter.search(query);

        StringQuery capturedQuery = queryCaptor.getValue();
        String json = capturedQuery.getSource();
        assertThat(json).contains("match_all");
        assertThat(json).doesNotContain("multi_match");
    }

    @Test
    @DisplayName("价格过滤应包含 range 查询")
    void search_shouldHandlePriceRange() {
        SearchHits<ProductDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);
        when(searchHits.getAggregations()).thenReturn(null);

        ArgumentCaptor<StringQuery> queryCaptor = ArgumentCaptor.forClass(StringQuery.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        ProductSearchQuery query = new ProductSearchQuery(null, null, null,
                new java.math.BigDecimal("100"), new java.math.BigDecimal("500"),
                null, null, 1, 20, null, false);
        adapter.search(query);

        StringQuery capturedQuery = queryCaptor.getValue();
        String json = capturedQuery.getSource();
        assertThat(json).contains("range");
        assertThat(json).contains("price");
    }

    @Test
    @DisplayName("排序参数应映射到正确字段")
    void search_shouldMapSortFields() {
        SearchHits<ProductDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);
        when(searchHits.getAggregations()).thenReturn(null);

        ArgumentCaptor<StringQuery> queryCaptor = ArgumentCaptor.forClass(StringQuery.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        ProductSearchQuery query = new ProductSearchQuery(null, null, null, null, null, null, "price_asc", 1, 20, null, false);
        adapter.search(query);

        StringQuery capturedQuery = queryCaptor.getValue();
        assertThat(capturedQuery.getSource()).contains("\"price\"", "\"asc\"");
    }
}
