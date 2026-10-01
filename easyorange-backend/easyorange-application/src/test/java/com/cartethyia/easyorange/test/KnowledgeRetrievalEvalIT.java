package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import com.cartethyia.easyorange.adapter.outbound.elasticsearch.KnowledgeChunkDocument;
import com.cartethyia.easyorange.ai.application.eval.GoldenSetLoader;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeMatch;
import com.cartethyia.easyorange.ai.domain.port.KnowledgeIndexPort;
import com.cartethyia.easyorange.ai.domain.port.RetrievalMetricPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.Queries;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;

/**
 * 知识库侧检索评测 + 三腿消融（kNN-only / BM25-only / RRF 融合），跑**真实 embedding** 语义空间。
 * <p>
 * 与 {@code AssetRetrievalEvalIT}（合成 one-hot 向量）构成本项目检索证据的两层口径，引用时不得混写：
 * 那一层证「融合机制有效 + kNN 路本身可用」，本 IT 证「真实语义空间的混合检索增益」——
 * 合成向量里同主题即余弦 1.0，量不出语义近邻对词面近邻的增益差。
 * <p>
 * 消融用例取 {@code eval/golden-set.yaml} 的 retrieval scope（含 retr-027~033 零词面重叠难用例）：
 * 词面重叠的用例 BM25 单路就能命中，三列对照无判别力；难用例里 BM25 必挂、kNN 是唯一出路。
 * 单腿查询构造与 {@code KnowledgeElasticsearchAdapter} 逐参对称（candidateK=topK×2、num_candidates=100、
 * title^2/content），改动任一侧都要同步另一侧，否则「同 except 单腿」的口径不成立。
 * <p>
 * 需要真实 embedding key 与 ES：无 key 静默跳过；ES 关闭会退化成 MySQL LIKE，本 IT 断言的三腿对照直接失效。
 */
@Slf4j
@EnabledIfEnvironmentVariable(named = "EMBEDDING_API_KEY", matches = ".+")
class KnowledgeRetrievalEvalIT extends AbstractIntegrationTest {

    private static final int TOP_K = 5;

    /** 与 KnowledgeElasticsearchAdapter.CANDIDATE_MULTIPLIER 同值：每路召回 topK×2 给融合留翻盘空间。 */
    private static final int CANDIDATE_K_MULTIPLIER = 2;

    private static final int NUM_CANDIDATES = 100;

    private static final int CANDIDATE_K = TOP_K * CANDIDATE_K_MULTIPLIER;

    /**
     * 零词面重叠难用例（对应 golden-set.yaml 里「难用例（真实语义层）」那一节的 retr-027~033）。
     * 只有这组适用「BM25 单路必挂」断言 —— 旧用例照抄文档标题原词，BM25 命中是应该的，
     * 拿它们一起断言必挂等于把评测集自己否掉。两档分开报数：旧用例量的是「用户照着标题问」的真实水平，
     * 难用例量的是「语义腿到底值多少钱」。
     */
    private static final Set<String> ZERO_OVERLAP_CASE_IDS =
            Set.of("retr-027", "retr-028", "retr-029", "retr-030", "retr-031", "retr-032", "retr-033");

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private KnowledgeIndexPort indexPort;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private GoldenSetLoader loader;

    @Autowired
    private RetrievalMetricPort metricPort;

    @Test
    @DisplayName("知识库检索评测：真实 embedding 下 kNN/BM25/RRF 三腿消融对照")
    void knowledgeRetrievalAblation() {
        var cases = loader.load().cases().stream()
                .filter(c -> GoldenSetLoader.SCOPE_RETRIEVAL.equals(c.scope()))
                .toList();
        assertThat(cases).as("金标准集应有 retrieval 用例").isNotEmpty();

        String runId = UUID.randomUUID().toString();
        var knnRrs = new ArrayList<Double>();
        var bm25Rrs = new ArrayList<Double>();
        var fusedRrs = new ArrayList<Double>();
        var caseIds = new ArrayList<String>();

        for (var c : cases) {
            float[] raw = embeddingModel.embed(c.question());
            assertThat(raw).as("用例 %s 的查询向量为空：kNN 腿不可用，三腿对照无意义", c.id()).isNotEmpty();
            List<Float> vector = toBoxed(raw);

            List<String> gold = c.goldDocIds();
            knnRrs.add(rr(gold, knnLegDocIds(vector)));
            bm25Rrs.add(rr(gold, bm25LegDocIds(c.question())));
            // 融合腿走生产端口（内部即 RrfFusion.fuse(DEFAULT_K, ...)），不在 IT 里重算融合
            double fusedRr = rr(
                    gold,
                    indexPort.search(c.question(), vector, TOP_K).stream()
                            .map(KnowledgeMatch::docId)
                            .toList());
            fusedRrs.add(fusedRr);
            caseIds.add(c.id());

            // 逐条落库只记融合腿（生产路径）：eo_retrieval_metric 按 run_id 聚合出趋势，单腿属消融日志
            metricPort.record(runId, c.id(), c.question(), String.join(",", gold), fusedRr > 0, fusedRr);
        }

        logAblationTable(caseIds, knnRrs, bm25Rrs, fusedRrs);

        // 必挂组的判别力断言：零词面重叠 → BM25 确定性 miss；融合腿含 BM25 这一路，排名不可能更差。
        // 不钉 kNN 必须命中：真实语义空间的召回质量本身就是被测对象，钉死等于把结论写成断言。
        for (int i = 0; i < caseIds.size(); i++) {
            String caseId = caseIds.get(i);
            assertThat(fusedRrs.get(i))
                    .as("%s：融合腿含 BM25 这一路，排名不应低于 BM25 单路", caseId)
                    .isGreaterThanOrEqualTo(bm25Rrs.get(i));
            if (ZERO_OVERLAP_CASE_IDS.contains(caseId)) {
                assertThat(bm25Rrs.get(i))
                        .as("%s：查询与 gold 零词面重叠（IK 切词后无公共 token），BM25 单路应确定性 miss", caseId)
                        .isZero();
            }
        }

        assertThat(hitRate(fusedRrs))
                .as("融合腿 hit@5 = %.2f（全量 %d 用例）", hitRate(fusedRrs), cases.size())
                .isGreaterThanOrEqualTo(0.5);
    }

    private void logAblationTable(List<String> caseIds, List<Double> knn, List<Double> bm25, List<Double> fused) {
        var table = new StringBuilder("\n[knowledge-retrieval-ablation] 三腿消融对照（%d 用例 × topK=%d，真实 embedding）\n"
                .formatted(caseIds.size(), TOP_K));
        var hard = new ArrayList<Integer>();
        for (int i = 0; i < caseIds.size(); i++) {
            if (ZERO_OVERLAP_CASE_IDS.contains(caseIds.get(i))) {
                hard.add(i);
            }
        }
        appendLegs(table, "全部", all(knn), all(bm25), all(fused));
        appendLegs(table, "零词面重叠难用例", pick(knn, hard), pick(bm25, hard), pick(fused, hard));
        for (int i = 0; i < caseIds.size(); i++) {
            table.append("  %-26s rr: kNN=%.2f BM25=%.2f fused=%.2f%n"
                    .formatted(caseIds.get(i), knn.get(i), bm25.get(i), fused.get(i)));
        }
        log.info("{}", table);
    }

    private static void appendLegs(
            StringBuilder table, String label, List<Double> knn, List<Double> bm25, List<Double> fused) {
        table.append("  [%s] n=%d\n".formatted(label, knn.size()));
        table.append("    leg        | hit@5 | MRR\n");
        table.append("    kNN-only   | %.2f  | %.4f\n".formatted(hitRate(knn), mrr(knn)));
        table.append("    BM25-only  | %.2f  | %.4f\n".formatted(hitRate(bm25), mrr(bm25)));
        table.append("    RRF fused  | %.2f  | %.4f\n".formatted(hitRate(fused), mrr(fused)));
    }

    private static List<Double> all(List<Double> values) {
        return List.copyOf(values);
    }

    private static List<Double> pick(List<Double> values, List<Integer> indexes) {
        return indexes.stream().map(values::get).toList();
    }

    // ── 消融单腿：与 KnowledgeElasticsearchAdapter 的 knnQuery / bm25Query 逐参对称 ──

    private List<String> knnLegDocIds(List<Float> vector) {
        var query = NativeQuery.builder()
                .withPageable(PageRequest.of(0, CANDIDATE_K))
                .withKnnSearches(knn -> knn.field("embedding")
                        .queryVector(vector)
                        .k(CANDIDATE_K)
                        .numCandidates(NUM_CANDIDATES))
                .withSort(byScoreDesc())
                .build();
        return searchDocIds(query);
    }

    private List<String> bm25LegDocIds(String queryText) {
        var query = NativeQuery.builder()
                .withPageable(PageRequest.of(0, CANDIDATE_K))
                .withQuery(Queries.wrapperQueryAsQuery("""
                        {"bool":{"must":[{"multi_match":{"query":"%s","type":"best_fields",\
                        "fuzziness":"AUTO","fields":["title^2","content"]}}]}}""".formatted(queryText.replace("\"", "\\\""))))
                .withSort(byScoreDesc())
                .build();
        return searchDocIds(query);
    }

    /**
     * 取 docId 而非 ES 文档 id：ES 侧 id 是 {@code docId:chunkIndex}，hit 口径按 docId 归并（与 GoldenSetEvaluator 同）。
     * <p>
     * <b>截断到 TOP_K 再交给评分</b>：单腿召回到 candidateK=10，但「只有这一腿时我们会返回什么」的可比口径是它的
     * top5 —— 融合腿也只留 5 条。不截断的话单腿按 10 条算 rr、三腿对照就成了 top10 vs top5，
     * MRR 会凭空高出融合腿一截（融合被 top5 截断、单腿没有）。
     */
    private List<String> searchDocIds(NativeQuery query) {
        return elasticsearchOperations.search(query, KnowledgeChunkDocument.class).getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(KnowledgeChunkDocument::getDocId)
                .distinct()
                .limit(TOP_K)
                .toList();
    }

    private static List<SortOptions> byScoreDesc() {
        return List.of(SortOptions.of(so -> so.score(s -> s.order(SortOrder.Desc))));
    }

    /** {@link EmbeddingModel#embed} 返回原始 float[]，ES 客户端要 List<Float>（与生产适配器同一装箱口径）。 */
    private static List<Float> toBoxed(float[] raw) {
        var boxed = new ArrayList<Float>(raw.length);
        for (float value : raw) {
            boxed.add(value);
        }
        return boxed;
    }

    /** MRR 分量：gold 首次出现在第 i 位（1 起）得 1/i，未命中为 0（与 GoldenSetEvaluator 同口径）。 */
    private static double rr(List<String> goldDocIds, List<String> rankedDocIds) {
        for (int i = 0; i < rankedDocIds.size(); i++) {
            if (goldDocIds.contains(rankedDocIds.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0;
    }

    private static double hitRate(List<Double> rrs) {
        return rrs.stream().filter(rr -> rr > 0).count() * 1.0 / rrs.size();
    }

    private static double mrr(List<Double> rrs) {
        return rrs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
}
