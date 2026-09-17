package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeChunk;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeMatch;
import com.cartethyia.easyorange.ai.domain.model.RrfFusion;
import com.cartethyia.easyorange.ai.domain.port.KnowledgeIndexPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.Queries;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 知识库向量索引（ES 适配器）— 实现 {@link KnowledgeIndexPort}。
 * <p>
 * 写入 best-effort（try-catch 只告警，索引失败不阻塞主链路，与商品索引同一语义）。
 * <p>
 * <b>检索 = 两路独立召回 + RRF 融合</b>：kNN（dense_vector，返回语义近邻排名）与
 * BM25（multi_match title^2/content，返回词面匹配排名）各查一次，再用 {@link RrfFusion}
 * 按排名融合。之所以是两次查询而不是一次「knn + query 同请求」：
 * <ul>
 *   <li>ES 同请求会把两路分数按内部规则合成一个分值，拿不到各自排名，无法做真正的排名融合；</li>
 *   <li>真正的融合要用排名而非分数 —— 余弦相似度与 BM25 分值量纲不可比。</li>
 * </ul>
 * 代价是两倍 ES 往返（小索引上是毫秒级），换来的是 BM25 的贡献能真正进最终排序，
 * 而不是只影响「谁进候选池」（此前 Java 侧按余弦重排会把 BM25 的排序信号整体丢掉，
 * 且重排用的还是同一 embedding 的同一余弦，对稠密那一路等价于没排）。
 * <p>
 * <b>不回传向量</b>：命中走 {@code _source} 排除 embedding（1024 维 float ≈ 每条 10KB），
 * 向量只在 ES 内部参与 kNN 打分。
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
public class KnowledgeElasticsearchAdapter implements KnowledgeIndexPort {

    /** 每路召回条数 = topK 的倍数：给融合留出「单路低位但另一路高位」的翻盘空间。 */
    private static final int CANDIDATE_MULTIPLIER = 2;

    /** kNN 的 num_candidates：ES 先按 ANN 取这么多候选再精确打分（远大于 k 才有效果）。 */
    private static final int NUM_CANDIDATES = 100;

    /** 索引里只取检索需要的字段（embedding 不回传，见类注释）。 */
    private static final SourceFilter SOURCE_FILTER =
            new FetchSourceFilterBuilder().withExcludes("embedding").build();

    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;

    @Override
    public void ingestChunks(List<KnowledgeChunk> chunks) {
        try {
            for (KnowledgeChunk chunk : chunks) {
                elasticsearchOperations.save(toDocument(chunk));
            }
        } catch (Exception e) {
            log.warn(
                    "Knowledge chunk ingest failed (best-effort), docId={}",
                    chunks.isEmpty() ? "?" : chunks.getFirst().docId(),
                    e);
        }
    }

    @Override
    public void removeDoc(String docId) {
        try {
            // 用类型化 term 查询构造，不拼 JSON 字符串：docId 目前由系统生成，
            // 但字符串拼接在调用方换成外部输入时会变成注入面
            var termQuery = Query.of(q -> q.term(t -> t.field("docId").value(FieldValue.of(docId))));
            var deleteQuery = DeleteQuery.builder(
                            NativeQuery.builder().withQuery(termQuery).build())
                    .build();
            elasticsearchOperations.delete(deleteQuery, KnowledgeChunkDocument.class);
        } catch (Exception e) {
            log.warn("Remove knowledge chunks failed (best-effort), docId={}", docId, e);
        }
    }

    @Override
    public List<KnowledgeMatch> search(String query, List<Float> queryEmbedding, int topK) {
        if (topK <= 0) {
            return List.of();
        }
        int candidateK = topK * CANDIDATE_MULTIPLIER;
        var docsById = new LinkedHashMap<String, KnowledgeChunkDocument>();
        var rankedLists = new ArrayList<List<String>>();

        if (queryEmbedding != null && !queryEmbedding.isEmpty()) {
            var knnLeg = runLeg(knnQuery(queryEmbedding, candidateK));
            docsById.putAll(knnLeg.docs());
            rankedLists.add(knnLeg.ids());
        }
        if (query != null && !query.isBlank()) {
            var bm25Leg = runLeg(bm25Query(query, candidateK));
            docsById.putAll(bm25Leg.docs());
            rankedLists.add(bm25Leg.ids());
        }
        if (rankedLists.isEmpty()) {
            return List.of();
        }

        return RrfFusion.fuse(RrfFusion.DEFAULT_K, rankedLists).stream()
                .limit(topK)
                .map(fused -> toMatch(docsById.get(fused.id()), fused.score()))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /** 单路召回结果：有序 ID 列表 + id → 文档（融合后按 id 回捞正文）。 */
    private record Leg(List<String> ids, Map<String, KnowledgeChunkDocument> docs) {}

    private Leg runLeg(NativeQuery esQuery) {
        try {
            var hits = elasticsearchOperations.search(esQuery, KnowledgeChunkDocument.class);
            var ids = new ArrayList<String>(hits.getSearchHits().size());
            var docs = new LinkedHashMap<String, KnowledgeChunkDocument>();
            for (SearchHit<KnowledgeChunkDocument> hit : hits.getSearchHits()) {
                ids.add(hit.getId());
                docs.put(hit.getId(), hit.getContent());
            }
            return new Leg(ids, docs);
        } catch (Exception e) {
            // 单路失败不影响另一路：退化为单路召回（语义检索优先于零结果）
            log.warn("Knowledge retrieval leg failed, falling back to single-leg ranking", e);
            return new Leg(List.of(), Map.of());
        }
    }

    private NativeQuery knnQuery(List<Float> queryEmbedding, int k) {
        return NativeQuery.builder()
                .withPageable(PageRequest.of(0, k))
                .withKnnSearches(knn ->
                        knn.field("embedding").queryVector(queryEmbedding).k(k).numCandidates(NUM_CANDIDATES))
                .withSourceFilter(SOURCE_FILTER)
                .withSort(byScoreDesc())
                .build();
    }

    private NativeQuery bm25Query(String query, int k) {
        return NativeQuery.builder()
                .withPageable(PageRequest.of(0, k))
                .withQuery(Queries.wrapperQueryAsQuery(buildBm25Query(query).toString()))
                .withSourceFilter(SOURCE_FILTER)
                .withSort(byScoreDesc())
                .build();
    }

    private static List<SortOptions> byScoreDesc() {
        return List.of(SortOptions.of(so -> so.score(s -> s.order(SortOrder.Desc))));
    }

    private JsonNode buildBm25Query(String query) {
        ObjectNode bool = objectMapper.createObjectNode();
        var must = objectMapper.createArrayNode();
        if (query != null && !query.isBlank()) {
            var multiMatch = objectMapper.createObjectNode();
            multiMatch.put("query", query);
            multiMatch.put("type", "best_fields");
            multiMatch.put("fuzziness", "AUTO");
            var fields = multiMatch.putArray("fields");
            fields.add("title^2");
            fields.add("content");
            must.add(objectMapper.createObjectNode().set("multi_match", multiMatch));
        } else {
            must.add(objectMapper.createObjectNode().set("match_all", objectMapper.createObjectNode()));
        }
        bool.set("must", must);
        return objectMapper.createObjectNode().set("bool", bool);
    }

    private static KnowledgeChunkDocument toDocument(KnowledgeChunk chunk) {
        return KnowledgeChunkDocument.builder()
                .id(chunk.docId() + ":" + chunk.chunkIndex())
                .docId(chunk.docId())
                .chunkIndex(chunk.chunkIndex())
                .title(chunk.title())
                .content(chunk.content())
                .embedding(chunk.embedding())
                .build();
    }

    private static KnowledgeMatch toMatch(KnowledgeChunkDocument doc, double score) {
        if (doc == null) {
            return null;
        }
        return new KnowledgeMatch(
                doc.getDocId(),
                doc.getChunkIndex() != null ? doc.getChunkIndex() : 0,
                doc.getTitle(),
                doc.getContent(),
                score);
    }
}
