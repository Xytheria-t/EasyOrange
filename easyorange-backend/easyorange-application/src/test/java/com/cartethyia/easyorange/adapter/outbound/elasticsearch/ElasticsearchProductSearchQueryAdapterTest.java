package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import co.elastic.clients.elasticsearch._types.SortOrder;
import com.cartethyia.easyorange.product.application.port.cache.SellerCachePort;
import com.cartethyia.easyorange.product.application.port.query.ProductSearchQueryPort.ProductSearchQuery;
import com.cartethyia.easyorange.product.application.port.query.SearchResult;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import tools.jackson.databind.ObjectMapper;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ElasticsearchProductSearchQueryAdapterTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private SellerCachePort sellerCachePort;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ElasticsearchProductSearchQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ElasticsearchProductSearchQueryAdapter(elasticsearchOperations, objectMapper, sellerCachePort);
    }

    @Test
    @DisplayName("无搜索结果时应返回空")
    void search_shouldReturnEmptyResultWhenNoResults() {
        SearchHits<ProductDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);
        when(searchHits.getAggregations()).thenReturn(null);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        ProductSearchQuery query =
                new ProductSearchQuery("test", null, null, null, null, null, null, 1, 20, null, false);
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
                .createTime(1735689600000L)
                .updateTime(1735776000000L)
                .build();

        SearchHit<ProductDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);

        SearchHits<ProductDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of(hit));
        when(searchHits.getTotalHits()).thenReturn(1L);
        when(searchHits.getAggregations()).thenReturn(null);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        ProductSearchQuery query =
                new ProductSearchQuery("test", null, null, null, null, null, null, 1, 20, null, false);
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

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        ProductSearchQuery query =
                new ProductSearchQuery("手机", "300", null, null, null, null, null, 1, 20, null, false);
        adapter.search(query);

        NativeQuery capturedQuery = queryCaptor.getValue();
        String dsl = decode(capturedQuery);
        assertThat(dsl).contains("categoryId");
        assertThat(dsl).contains("300");
    }

    @Test
    @DisplayName("空关键词应执行 match_all 查询")
    void search_shouldUseMatchAllForBlankKeyword() {
        SearchHits<ProductDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);
        when(searchHits.getAggregations()).thenReturn(null);

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        ProductSearchQuery query = new ProductSearchQuery(null, null, null, null, null, null, null, 1, 20, null, false);
        adapter.search(query);

        NativeQuery capturedQuery = queryCaptor.getValue();
        String dsl = decode(capturedQuery);
        assertThat(dsl).contains("match_all");
        assertThat(dsl).doesNotContain("multi_match");
    }

    @Test
    @DisplayName("价格过滤应包含 range 查询")
    void search_shouldHandlePriceRange() {
        SearchHits<ProductDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);
        when(searchHits.getAggregations()).thenReturn(null);

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        ProductSearchQuery query = new ProductSearchQuery(
                null,
                null,
                null,
                new java.math.BigDecimal("100"),
                new java.math.BigDecimal("500"),
                null,
                null,
                1,
                20,
                null,
                false);
        adapter.search(query);

        NativeQuery capturedQuery = queryCaptor.getValue();
        String dsl = decode(capturedQuery);
        assertThat(dsl).contains("range");
        assertThat(dsl).contains("price");
    }

    @Test
    @DisplayName("排序参数应映射到正确字段")
    void search_shouldMapSortFields() {
        SearchHits<ProductDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);
        when(searchHits.getAggregations()).thenReturn(null);

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        ProductSearchQuery query =
                new ProductSearchQuery(null, null, null, null, null, null, "price_asc", 1, 20, null, false);
        adapter.search(query);

        NativeQuery capturedQuery = queryCaptor.getValue();
        assertThat(capturedQuery.getSortOptions()).singleElement().satisfies(so -> {
            assertThat(so.field().field()).isEqualTo("price");
            assertThat(so.field().order()).isEqualTo(SortOrder.Asc);
        });
    }

    @Test
    @DisplayName("有查询向量且按相关性排序时发出两路召回：kNN 一路 + BM25 一路")
    void search_withEmbedding_shouldIssueTwoLegs() {
        List<NativeQuery> issued = new ArrayList<>();
        var knnHits = hits(List.of(hit(doc("1", "商品1"), "1")), 1L);
        var bm25Hits = hits(List.of(hit(doc("1", "商品1"), "1")), 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenAnswer(inv -> {
                    NativeQuery q = inv.getArgument(0);
                    issued.add(q);
                    return q.getKnnSearches().isEmpty() ? bm25Hits : knnHits;
                });

        SearchResult result = adapter.search(twoLegQuery(null));

        assertThat(issued).hasSize(2);
        assertThat(issued.stream().filter(q -> !q.getKnnSearches().isEmpty())).hasSize(1);
        assertThat(issued.stream().filter(q -> q.getKnnSearches().isEmpty())).hasSize(1);
        assertThat(result.records()).hasSize(1);
    }

    @Test
    @DisplayName("融合取两路并集并按排名重排：两路都靠前的排第一，只被一路召回的仍保留")
    void search_shouldFuseLegsByRank() {
        // kNN 路：[A, B]；BM25 路：[A, C] —— A 两路都第 1，C 只被 BM25 召回
        var knnHits = hits(List.of(hit(doc("A", "商品A"), "A"), hit(doc("B", "商品B"), "B")), 2L);
        var bm25Hits = hits(List.of(hit(doc("A", "商品A"), "A"), hit(doc("C", "商品C"), "C")), 2L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenAnswer(inv ->
                        ((NativeQuery) inv.getArgument(0)).getKnnSearches().isEmpty() ? bm25Hits : knnHits);

        SearchResult result = adapter.search(twoLegQuery(null));

        assertThat(result.records()).extracting(r -> r.id()).containsExactly("A", "B", "C");
    }

    @Test
    @DisplayName("total 取 BM25 路的总命中：kNN 那路只返回候选池条数，不能拿来当总数")
    void search_totalShouldComeFromBm25Leg() {
        var knnHits = hits(List.of(hit(doc("1", "商品1"), "1")), 200L);
        var bm25Hits = hits(List.of(hit(doc("1", "商品1"), "1")), 7L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenAnswer(inv ->
                        ((NativeQuery) inv.getArgument(0)).getKnnSearches().isEmpty() ? bm25Hits : knnHits);

        SearchResult result = adapter.search(twoLegQuery(null));

        assertThat(result.total()).isEqualTo(7);
    }

    @Test
    @DisplayName("单路失败不抛出：kNN 挂了仍返回 BM25 一路的结果")
    void search_shouldDegradeToSingleLegWhenOneLegFails() {
        var bm25Hits = hits(List.of(hit(doc("1", "商品1"), "1")), 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenAnswer(inv -> {
                    if (!((NativeQuery) inv.getArgument(0)).getKnnSearches().isEmpty()) {
                        throw new IllegalStateException("knn leg down");
                    }
                    return bm25Hits;
                });

        SearchResult result = adapter.search(twoLegQuery(null));

        assertThat(result.records()).extracting(r -> r.id()).containsExactly("1");
    }

    @Test
    @DisplayName("显式排序时即使带向量也只发一路：融合排名会和点选的排序打架")
    void search_withExplicitSort_shouldIssueSingleLeg() {
        // 单路路径只按文档内容映射，不读 hit.id（_id 只有两路召回回捞时才用得上）
        SearchHit<ProductDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc("1", "商品1"));
        var bm25Hits = hits(List.of(hit), 1L);
        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ProductDocument.class)))
                .thenReturn(bm25Hits);

        adapter.search(twoLegQuery("price_asc"));

        assertThat(queryCaptor.getAllValues()).hasSize(1);
        assertThat(queryCaptor.getValue().getKnnSearches()).isEmpty();
        assertThat(queryCaptor.getValue().getSortOptions())
                .singleElement()
                .satisfies(so -> assertThat(so.field().field()).isEqualTo("price"));
    }

    @Test
    @DisplayName("kNN 路的 num_candidates 必须 ≥ k，否则 ES 直接拒收整个请求")
    void search_knnLeg_shouldKeepNumCandidatesAtLeastK() {
        List<NativeQuery> issued = new ArrayList<>();
        var hitsMock = hits(List.of(), 0L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenAnswer(inv -> {
                    NativeQuery q = inv.getArgument(0);
                    issued.add(q);
                    return hitsMock;
                });

        adapter.search(twoLegQuery(null));

        var knnLeg = issued.stream()
                .filter(q -> !q.getKnnSearches().isEmpty())
                .findFirst()
                .orElseThrow();
        var knn = knnLeg.getKnnSearches().get(0);
        assertThat(knn.k()).isGreaterThan(0);
        assertThat(knn.numCandidates()).isGreaterThanOrEqualTo(knn.k());
    }

    @Test
    @DisplayName("词面命中为 0 但语义路有召回时不能报 total=0：否则前端显示「共找到 0 件」却列着卡片")
    void search_shouldNotReportZeroTotalWhenOnlySemanticLegMatches() {
        var knnHits = hits(List.of(hit(doc("A", "商品A"), "A"), hit(doc("B", "商品B"), "B")), 2L);
        var bm25Hits = hits(List.of(), 0L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenAnswer(inv ->
                        ((NativeQuery) inv.getArgument(0)).getKnnSearches().isEmpty() ? bm25Hits : knnHits);

        SearchResult result = adapter.search(twoLegQuery(null));

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.records()).extracting(r -> r.id()).containsExactly("A", "B");
    }

    private static ProductSearchQuery twoLegQuery(String sort) {
        return new ProductSearchQuery("手机", null, "ONLINE", null, null, null, sort, 1, 20, List.of(0.1f, 0.2f), true);
    }

    private static ProductDocument doc(String id, String name) {
        return ProductDocument.builder()
                .id(id)
                .name(name)
                .status(ProductStatus.ONLINE.getCode())
                .createTime(1735689600000L)
                .build();
    }

    private static SearchHit<ProductDocument> hit(ProductDocument doc, String id) {
        SearchHit<ProductDocument> hit = mock(SearchHit.class);
        when(hit.getId()).thenReturn(id);
        when(hit.getContent()).thenReturn(doc);
        return hit;
    }

    private static SearchHits<ProductDocument> hits(List<SearchHit<ProductDocument>> content, long total) {
        SearchHits<ProductDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(content);
        when(searchHits.getTotalHits()).thenReturn(total);
        return searchHits;
    }

    /** NativeQuery 把 query DSL 以 wrapper 查询承载（base64），解码后断言其内容 */
    private static String decode(NativeQuery nativeQuery) {
        return new String(
                Base64.getDecoder().decode(nativeQuery.getQuery().wrapper().query()), StandardCharsets.UTF_8);
    }
}
