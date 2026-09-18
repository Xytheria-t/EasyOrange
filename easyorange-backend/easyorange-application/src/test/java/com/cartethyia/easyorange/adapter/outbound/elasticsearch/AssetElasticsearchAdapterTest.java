package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.port.AssetRetrievalPort;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import tools.jackson.databind.ObjectMapper;

/**
 * 在售资产检索适配器测试 —— 与 {@code KnowledgeElasticsearchAdapterTest} 同构，
 * 因为这两条链路刻意保持同一形态（两路独立召回 + RRF 排名融合）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("在售资产 ES 适配器 -> 测试")
class AssetElasticsearchAdapterTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private AssetRetrievalPort adapter() {
        return new AssetElasticsearchAdapter(elasticsearchOperations, new ObjectMapper());
    }

    @Test
    @DisplayName("ES 启用时 isAvailable=true")
    void available() {
        assertThat(adapter().isAvailable()).isTrue();
    }

    @Test
    @DisplayName("检索 -> 两路各查一次，RRF 融合后两路都命中的排最前")
    void search_fusesBothLegs() {
        var adapter = adapter();
        // 两条腿的返回必须在 when 之外造好：造桩时内部又调 when(...) 会触发 Mockito 的嵌套 stubbing 报错
        var knnLeg = hits("p-1", "p-2");
        var bm25Leg = hits("p-2", "p-3");
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(knnLeg)
                .thenReturn(bm25Leg);

        List<AssetHit> hits = adapter.search("5000 以内笔记本", List.of(1f, 0f), 3);

        // p-2 两路都命中 → 融合分最高；其后按各路名次（1/61 > 1/62）
        assertThat(hits).extracting(AssetHit::productId).containsExactly("p-2", "p-1", "p-3");
        assertThat(hits.getFirst().score()).isGreaterThan(hits.get(1).score());
        verify(elasticsearchOperations, times(2)).search(any(NativeQuery.class), eq(ProductDocument.class));
    }

    @Test
    @DisplayName("单路失败 -> 另一路结果照常返回（不整体空手而归）")
    void search_singleLegFailure_degrades() {
        var adapter = adapter();
        var bm25Leg = hits("p-9");
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenThrow(new RuntimeException("knn timeout"))
                .thenReturn(bm25Leg);

        assertThat(adapter.search("笔记本", List.of(1f, 0f), 3))
                .extracting(AssetHit::productId)
                .containsExactly("p-9");
    }

    @Test
    @DisplayName("无查询向量 -> 只查词面路一次（embedding 抖动不该让找货整体失效）")
    void search_withoutVector_usesBm25Only() {
        var adapter = adapter();
        var bm25Leg = hits("p-7");
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(bm25Leg);

        assertThat(adapter.search("笔记本", null, 3))
                .extracting(AssetHit::productId)
                .containsExactly("p-7");
        verify(elasticsearchOperations, times(1)).search(any(NativeQuery.class), eq(ProductDocument.class));
    }

    @Test
    @DisplayName("无关键词 -> 只查向量路一次")
    void search_withoutKeyword_usesKnnOnly() {
        var adapter = adapter();
        var knnLeg = hits("p-5");
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(knnLeg);

        assertThat(adapter.search("  ", List.of(1f, 0f), 3))
                .extracting(AssetHit::productId)
                .containsExactly("p-5");
        verify(elasticsearchOperations, times(1)).search(any(NativeQuery.class), eq(ProductDocument.class));
    }

    @Test
    @DisplayName("topK <= 0 -> 空列表且不查 ES")
    void search_zeroTopK() {
        assertThat(adapter().search("笔记本", List.of(1f, 0f), 0)).isEmpty();
        verify(elasticsearchOperations, never()).search(any(NativeQuery.class), eq(ProductDocument.class));
    }

    @Test
    @DisplayName("两路召回都带 status=ONLINE 过滤（只过滤一路会把不可售资产带进候选池）")
    void search_bothLegsFilterOnline() {
        var adapter = (AssetElasticsearchAdapter) adapter();

        // kNN 那路传 null 取纯过滤体，BM25 那路带关键词 —— 两条腿共用这个构造器，
        // 所以断言它即可覆盖两路（穿透 mock 去读 NativeQuery 的请求体读不到 wrapper 里的 JSON）
        assertThat(adapter.buildBm25Query(null).toString())
                .as("kNN 路的过滤体")
                .contains("ONLINE")
                .contains("match_all");
        assertThat(adapter.buildBm25Query("笔记本").toString())
                .as("BM25 路的请求体")
                .contains("ONLINE")
                .contains("multi_match", "笔记本");
    }

    @Test
    @DisplayName("成色码 -> 中文描述；码表里没有的原样透出，不吞信息")
    void toHit_mapsConditionCode() {
        var adapter = adapter();
        var leg = hits(doc("p-1", "2"), doc("p-2", "NOT_A_CODE"));
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(leg);

        List<AssetHit> hits = adapter.search("笔记本", List.of(1f, 0f), 3);

        assertThat(hits).extracting(AssetHit::productId).containsExactly("p-1", "p-2");
        assertThat(hits.get(0).conditionDesc()).as("成色码 2 应解析为中文描述").isEqualTo("几乎全新");
        assertThat(hits.get(1).conditionDesc()).as("码表外的码原样透出").isEqualTo("NOT_A_CODE");
    }

    private static ProductDocument doc(String id) {
        return doc(id, null);
    }

    private static ProductDocument doc(String id, String conditionCode) {
        return ProductDocument.builder()
                .id(id)
                .name("资产-" + id)
                .description("描述-" + id)
                .categoryName("数码")
                .price(4200.0)
                .conditionLevel(conditionCode)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static SearchHits<ProductDocument> hits(String... ids) {
        var docs = Arrays.stream(ids).map(AssetElasticsearchAdapterTest::doc).toArray(ProductDocument[]::new);
        return hits(docs);
    }

    private static SearchHits<ProductDocument> hits(ProductDocument... docs) {
        List<SearchHit<ProductDocument>> searchHits = Arrays.stream(docs)
                .map(d -> {
                    SearchHit<ProductDocument> hit = mock(SearchHit.class);
                    when(hit.getId()).thenReturn(d.getId());
                    when(hit.getContent()).thenReturn(d);
                    return hit;
                })
                .toList();
        SearchHits<ProductDocument> result = mock(SearchHits.class);
        when(result.getSearchHits()).thenReturn(searchHits);
        return result;
    }
}
