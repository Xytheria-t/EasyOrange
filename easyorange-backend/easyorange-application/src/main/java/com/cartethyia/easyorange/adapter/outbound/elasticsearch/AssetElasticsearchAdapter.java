package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.RrfFusion;
import com.cartethyia.easyorange.ai.domain.port.AssetRetrievalPort;
import java.math.BigDecimal;
import java.time.Duration;
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
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 在售资产检索（ES 适配器）— 实现 {@link AssetRetrievalPort}。
 * <p>
 * <b>与 {@link KnowledgeElasticsearchAdapter} 刻意对称：两路独立召回 + RRF 排名融合。</b>
 * kNN（{@code nameEmbedding}，dense_vector）与 BM25（{@code multi_match name^3/description}）各查一次，
 * 再用 {@link RrfFusion} 按排名融合。同请求里「knn + query」不行 —— ES 会把两路分数按内部规则合成
 * 一个分值，拿不到各自排名；而余弦相似度与 BM25 分值量纲不可比，只有排名能融。
 * <p>
 * <b>只召回 {@code ONLINE} 资产</b>：对话里推荐的每一条都得当下可下单，把草稿 / 待审核 / 已售资产
 * 推给用户是坏演示。过滤条件在两路召回里都带上（不能只过滤一路，否则漏掉的那路会把不可售资产带进候选池）。
 * <p>
 * 单路失败不影响另一路：任一路抛异常就退化为单路召回（与知识库同一语义）。
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AssetElasticsearchAdapter implements AssetRetrievalPort {

    /** 每路召回条数 = topK 的倍数：给融合留出「单路低位但另一路高位」的翻盘空间。 */
    private static final int CANDIDATE_MULTIPLIER = 2;

    /** kNN 的 num_candidates：ES 先按 ANN 取这么多候选再精确打分（远大于 k 才有效果）。 */
    private static final int NUM_CANDIDATES = 100;

    private static final String STATUS_ONLINE = "ONLINE";

    /** 索引里只取检索需要的字段（1024 维向量不回传，只在 ES 内部参与打分）。 */
    private static final SourceFilter SOURCE_FILTER =
            new FetchSourceFilterBuilder().withExcludes("nameEmbedding").build();

    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;
    private final SearchLegMetrics legMetrics;

    @Override
    public List<AssetHit> search(String query, List<Float> queryEmbedding, int topK) {
        if (topK <= 0) {
            return List.of();
        }
        int candidateK = topK * CANDIDATE_MULTIPLIER;
        var docsById = new LinkedHashMap<String, ProductDocument>();
        var rankedLists = new ArrayList<List<String>>();

        if (queryEmbedding != null && !queryEmbedding.isEmpty()) {
            var knnLeg = runLeg("knn", knnQuery(queryEmbedding, candidateK));
            docsById.putAll(knnLeg.docs());
            rankedLists.add(knnLeg.ids());
        }
        if (query != null && !query.isBlank()) {
            var bm25Leg = runLeg("bm25", bm25Query(query, candidateK));
            docsById.putAll(bm25Leg.docs());
            rankedLists.add(bm25Leg.ids());
        }
        if (rankedLists.isEmpty()) {
            return List.of();
        }

        return RrfFusion.fuse(RrfFusion.DEFAULT_K, rankedLists).stream()
                .limit(topK)
                .map(fused -> toHit(docsById.get(fused.id()), fused.score()))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /** 单路召回结果：有序 ID 列表 + id → 文档（融合后按 id 回捞字段）。 */
    private record Leg(List<String> ids, Map<String, ProductDocument> docs) {}

    private Leg runLeg(String leg, NativeQuery esQuery) {
        long start = System.nanoTime();
        try {
            var hits = elasticsearchOperations.search(esQuery, ProductDocument.class);
            var ids = new ArrayList<String>(hits.getSearchHits().size());
            var docs = new LinkedHashMap<String, ProductDocument>();
            for (SearchHit<ProductDocument> hit : hits.getSearchHits()) {
                ids.add(hit.getId());
                docs.put(hit.getId(), hit.getContent());
            }
            legMetrics.record("asset", leg, Duration.ofNanos(System.nanoTime() - start), true);
            return new Leg(ids, docs);
        } catch (Exception e) {
            // 单路失败不影响另一路：退化为单路召回（向量那路优先于零结果）
            log.warn("Asset retrieval leg failed, falling back to single-leg ranking", e);
            legMetrics.record("asset", leg, Duration.ofNanos(System.nanoTime() - start), false);
            return new Leg(List.of(), Map.of());
        }
    }

    /**
     * kNN 腿：{@code status=ONLINE} 过滤走 {@code knn.filter}（预过滤），<b>不得挂顶层 query</b>。
     * <p>
     * 实测（2026-09-23，scripts/repro-td-020-knn-threshold.py 同源探针）：顶层 query 与 kNN 是
     * <b>纯并集</b>——kNN 候选完全不受 query 过滤（零命中过滤仍返回满额候选），DRAFT/REJECTED 会
     * 带着向量分进候选池；且一旦给这条腿补 {@code knn.similarity}，顶层 query（match_all+过滤）
     * 会把阈值整个架空（TD-020 四组对照的 g3：乱码返全库）。商品腿同一形态见
     * {@link ElasticsearchProductSearchQueryAdapter#knnQuery}。
     */
    private NativeQuery knnQuery(List<Float> queryEmbedding, int k) {
        return NativeQuery.builder()
                .withPageable(PageRequest.of(0, k))
                .withKnnSearches(knn -> {
                    knn.field("nameEmbedding")
                            .queryVector(queryEmbedding)
                            .k(k)
                            .numCandidates(NUM_CANDIDATES)
                            .filter(Queries.wrapperQueryAsQuery(
                                    onlineOnlyFilter().toString()));
                    return knn;
                })
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

    /**
     * BM25 子句（{@code query} 非空由调用方保证——{@link #search} 只在关键词非空时发这条腿）。
     * <p>
     * 两条路都带 {@code status = ONLINE} 过滤：只过滤一路会让不过滤的那路把不可售资产带进候选池，
     * 融合后照样可能出现在推荐里（kNN 那路经 {@link #knnQuery} 的 {@code knn.filter} 承载）。
     * 包级可见，供测试直接断言过滤条件。
     */
    JsonNode buildBm25Query(String query) {
        ObjectNode bool = objectMapper.createObjectNode();

        var must = objectMapper.createArrayNode();
        var multiMatch = objectMapper.createObjectNode();
        multiMatch.put("query", query);
        multiMatch.put("type", "best_fields");
        multiMatch.put("fuzziness", "AUTO");
        var fields = multiMatch.putArray("fields");
        fields.add("name^3");
        fields.add("description");
        must.add(objectMapper.createObjectNode().set("multi_match", multiMatch));
        bool.set("must", must);
        bool.set("filter", onlineFilterClauses());

        return objectMapper.createObjectNode().set("bool", bool);
    }

    /** kNN 腿的预过滤体 {@code {"bool":{"filter":[{"term":{"status":"ONLINE"}}]}}}（wrapper 装查询对象，装数组会被 ES 拒收）。 */
    private JsonNode onlineOnlyFilter() {
        return objectMapper
                .createObjectNode()
                .set("bool", objectMapper.createObjectNode().set("filter", onlineFilterClauses()));
    }

    /** 两腿共用的 {@code status=ONLINE} 过滤子句 — 定义只此一处，改口径两腿同时生效。 */
    private ArrayNode onlineFilterClauses() {
        var term = objectMapper.createObjectNode().put("status", STATUS_ONLINE);
        return objectMapper
                .createArrayNode()
                .add(objectMapper.createObjectNode().set("term", term));
    }

    private static AssetHit toHit(ProductDocument doc, double score) {
        if (doc == null) {
            return null;
        }
        return new AssetHit(
                doc.getId(),
                doc.getName(),
                doc.getPrice() != null ? BigDecimal.valueOf(doc.getPrice()) : null,
                doc.getCategoryName(),
                ConditionLevelText.of(doc.getConditionLevel()),
                score);
    }
}
