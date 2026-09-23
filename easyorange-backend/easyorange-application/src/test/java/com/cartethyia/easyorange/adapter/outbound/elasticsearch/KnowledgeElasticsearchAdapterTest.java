package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.domain.model.KnowledgeChunk;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeMatch;
import com.cartethyia.easyorange.ai.domain.port.KnowledgeIndexPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("知识库 ES 适配器 -> 测试")
class KnowledgeElasticsearchAdapterTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Test
    @DisplayName("写入分块 -> 逐块 save（id = docId:chunkIndex 幂等覆盖）")
    void ingestChunks() {
        KnowledgeIndexPort adapter = new KnowledgeElasticsearchAdapter(
                elasticsearchOperations, new ObjectMapper(), new SearchLegMetrics(new SimpleMeterRegistry()));

        adapter.ingestChunks(List.of(
                new KnowledgeChunk("kb-1", 0, "标题", "块0", List.of(0.1f, 0.2f)),
                new KnowledgeChunk("kb-1", 1, "标题", "块1", null)));

        verify(elasticsearchOperations, org.mockito.Mockito.times(2)).save(any(KnowledgeChunkDocument.class));
    }

    @Test
    @DisplayName("删除文档 -> 按 docId term 删除（best-effort 不抛）")
    void removeDoc() {
        KnowledgeIndexPort adapter = new KnowledgeElasticsearchAdapter(
                elasticsearchOperations, new ObjectMapper(), new SearchLegMetrics(new SimpleMeterRegistry()));

        adapter.removeDoc("kb-1");

        verify(elasticsearchOperations).delete(any(DeleteQuery.class), any(Class.class));
    }

    @Test
    @DisplayName("写入失败 -> best-effort 不抛异常（索引失败不阻塞主链路）")
    void ingestChunks_bestEffort() {
        when(elasticsearchOperations.save(any(KnowledgeChunkDocument.class)))
                .thenThrow(new RuntimeException("es down"));
        KnowledgeIndexPort adapter = new KnowledgeElasticsearchAdapter(
                elasticsearchOperations, new ObjectMapper(), new SearchLegMetrics(new SimpleMeterRegistry()));

        adapter.ingestChunks(List.of(new KnowledgeChunk("kb-1", 0, "标题", "块0", null)));
    }

    @Test
    @DisplayName("ES 启用时 isAvailable=true")
    void available() {
        KnowledgeIndexPort adapter = new KnowledgeElasticsearchAdapter(
                mock(ElasticsearchOperations.class),
                new ObjectMapper(),
                new SearchLegMetrics(new SimpleMeterRegistry()));

        assertThat(adapter.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("检索 -> 两路各查一次，RRF 融合后两路都命中的排最前")
    void search_fusesBothLegs() {
        KnowledgeIndexPort adapter = new KnowledgeElasticsearchAdapter(
                elasticsearchOperations, new ObjectMapper(), new SearchLegMetrics(new SimpleMeterRegistry()));
        // 先造好两条腿的返回（在 when 之外，避免嵌套 stubbing），第一次调用 = kNN 路，第二次 = BM25 路
        var knnLeg = hits("kb-1:0", "kb-2:0");
        var bm25Leg = hits("kb-2:0", "kb-3:0");
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(KnowledgeChunkDocument.class)))
                .thenReturn(knnLeg)
                .thenReturn(bm25Leg);

        List<KnowledgeMatch> matches = adapter.search("退款", List.of(1f, 0f), 3);

        // kb-2 两路都命中 → 融合分最高；其后按各路名次（1/61 > 1/62）
        assertThat(matches).extracting(KnowledgeMatch::docId).containsExactly("kb-2", "kb-1", "kb-3");
        assertThat(matches.getFirst().score()).isGreaterThan(matches.get(1).score());
        verify(elasticsearchOperations, times(2)).search(any(NativeQuery.class), eq(KnowledgeChunkDocument.class));
    }

    @Test
    @DisplayName("单路失败 -> 另一路结果照常返回（不整体空手而归）")
    void search_singleLegFailure_degrades() {
        KnowledgeIndexPort adapter = new KnowledgeElasticsearchAdapter(
                elasticsearchOperations, new ObjectMapper(), new SearchLegMetrics(new SimpleMeterRegistry()));
        var bm25Leg = hits("kb-9:0");
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(KnowledgeChunkDocument.class)))
                .thenThrow(new RuntimeException("knn timeout"))
                .thenReturn(bm25Leg);

        List<KnowledgeMatch> matches = adapter.search("退款", List.of(1f, 0f), 3);

        assertThat(matches).extracting(KnowledgeMatch::docId).containsExactly("kb-9");
    }

    @Test
    @DisplayName("无查询向量 -> 只查词面路一次（不空手而归）")
    void search_withoutVector_usesBm25Only() {
        KnowledgeIndexPort adapter = new KnowledgeElasticsearchAdapter(
                elasticsearchOperations, new ObjectMapper(), new SearchLegMetrics(new SimpleMeterRegistry()));
        var bm25Leg = hits("kb-7:0");
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(KnowledgeChunkDocument.class)))
                .thenReturn(bm25Leg);

        List<KnowledgeMatch> matches = adapter.search("退款", null, 3);

        assertThat(matches).extracting(KnowledgeMatch::docId).containsExactly("kb-7");
        verify(elasticsearchOperations, times(1)).search(any(NativeQuery.class), eq(KnowledgeChunkDocument.class));
    }

    private static KnowledgeChunkDocument doc(String esId) {
        String docId = esId.substring(0, esId.indexOf(':'));
        return KnowledgeChunkDocument.builder()
                .id(esId)
                .docId(docId)
                .chunkIndex(0)
                .title("标题-" + docId)
                .content("正文-" + docId)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static SearchHits<KnowledgeChunkDocument> hits(String... esIds) {
        List<SearchHit<KnowledgeChunkDocument>> searchHits = Arrays.stream(esIds)
                .map(esId -> {
                    SearchHit<KnowledgeChunkDocument> hit = mock(SearchHit.class);
                    when(hit.getId()).thenReturn(esId);
                    when(hit.getContent()).thenReturn(doc(esId));
                    return hit;
                })
                .toList();
        SearchHits<KnowledgeChunkDocument> result = mock(SearchHits.class);
        when(result.getSearchHits()).thenReturn(searchHits);
        return result;
    }
}
