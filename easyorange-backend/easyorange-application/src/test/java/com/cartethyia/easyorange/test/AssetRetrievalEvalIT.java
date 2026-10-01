package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.cartethyia.easyorange.adapter.outbound.elasticsearch.ProductDocument;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.port.AssetRetrievalPort;
import com.cartethyia.easyorange.ai.domain.port.RetrievalMetricPort;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.Queries;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * asset 找货链路检索评测 + 三腿消融（kNN-only / BM25-only / RRF 融合）——AssetSourcing 此前零检索质量评测，
 * 工程指标「混合检索价值证据待补口径」就地销账，eo_retrieval_metric 的 asset- 前缀用例即 ADR-0012 回看数据源。
 * 零真实 key：语料直写 ES products 索引，nameEmbedding 按主题人工构造 one-hot（1024 维与 mapping 锁死一致），
 * gold 与查询共用同主题向量 → kNN 召回确定性可预测；共享 dev 索引以 MARKER 隔离，finally 按 ID 清理。
 * <p>口径分层（引用数字不得混写）：本 IT 证明「融合机制有效 + kNN 路本身可用」；真实语义空间的 kNN 增益
 * 仍需真实 embedding 的语义层评测（付费 dispatch）。hit@5 下限内联对齐 eval/baselines.yaml 的 min-hit-at-5。
 */
@Slf4j
@ActiveProfiles({"it", "it-es"})
class AssetRetrievalEvalIT extends AbstractIntegrationTest {

    /** 独占标记词：全部语料标题前缀，计数敏感断言只认它（真实词会撞共享索引里的存量商品）。 */
    private static final String MARKER = "ITAREV7";

    private static final int TOP_K = 5;

    /** 与 eval/baselines.yaml 的 retrieval.min-hit-at-5 同一口径：同一道门禁，不让 IT 与基线各说各话。 */
    private static final double MIN_HIT_AT_5 = 0.5;

    private static final int NUM_CANDIDATES = 100;

    private static final int EMBEDDING_DIMS = 1024;

    /** kNN 预过滤体，与 {@code AssetElasticsearchAdapter} 同形（过滤必须挂 knn.filter，理由见其类注释）。 */
    private static final String ONLINE_ONLY_FILTER = "{\"bool\":{\"filter\":[{\"term\":{\"status\":\"ONLINE\"}}]}}";

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private AssetRetrievalPort assetRetrievalPort;

    @Autowired
    private RetrievalMetricPort metricPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record CorpusDoc(String id, String name, String description, int topicDim) {}

    private record EvalCase(String caseId, String query, int queryTopicDim, String goldId, boolean bm25MustMiss) {}

    @Test
    @DisplayName("asset 检索评测：hit@5/MRR 落库 + kNN/BM25/RRF 三腿消融对照")
    void retrievalEvalWithThreeLegAblation() {
        var docs = corpus();
        try {
            // ── 写语料 → refresh → MARKER 计数守门：语料不可见时跑评测只会得到假 miss ──
            docs.forEach(this::save);
            elasticsearchOperations.indexOps(ProductDocument.class).refresh();
            assertThat(countByMarker()).as("语料应全部可见（refresh 后按 MARKER 计数）").isEqualTo(docs.size());

            var cases = cases();
            String runId = UUID.randomUUID().toString();
            var knnRrs = new ArrayList<Double>();
            var bm25Rrs = new ArrayList<Double>();
            var fusedRrs = new ArrayList<Double>();

            // ── 三腿同跑：融合腿即生产路径（adapter 内部 RrfFusion.fuse(k=60)），单腿为消融对照 ──
            for (var c : cases) {
                List<Float> vector = topicVector(c.queryTopicDim());
                knnRrs.add(reciprocalRank(c.goldId(), knnLegIds(vector)));
                bm25Rrs.add(reciprocalRank(c.goldId(), bm25LegIds(c.query())));
                List<AssetHit> fused = assetRetrievalPort.search(c.query(), vector, TOP_K);
                fusedRrs.add(reciprocalRank(
                        c.goldId(), fused.stream().map(AssetHit::productId).toList()));
                // 逐条落库只记融合腿（生产路径）：eo_retrieval_metric 按 run_id 聚合出趋势，单腿属消融日志
                metricPort.record(runId, c.caseId(), c.query(), c.goldId(), fusedRrs.getLast() > 0, fusedRrs.getLast());
            }

            logAblationTable(cases, knnRrs, bm25Rrs, fusedRrs);

            double fusedHitRate = hitRate(fusedRrs);
            assertThat(fusedHitRate)
                    .as("融合腿 hit@5 %.2f 低于内联下限 %.2f（口径对齐 baselines.yaml min-hit-at-5）", fusedHitRate, MIN_HIT_AT_5)
                    .isGreaterThanOrEqualTo(MIN_HIT_AT_5);

            // 必挂组的判别力断言：零词面重叠 → BM25 确定性 miss；同主题向量 → kNN 确定性 hit；
            // 融合腿捞回 —— 「混合检索价值」在本语料空间的可复现证据，三列差异即消融结论
            for (int i = 0; i < cases.size(); i++) {
                var c = cases.get(i);
                if (!c.bm25MustMiss()) {
                    continue;
                }
                assertThat(bm25Rrs.get(i))
                        .as("%s：gold 与查询零词面重叠，BM25 单路应确定性 miss", c.caseId())
                        .isZero();
                assertThat(knnRrs.get(i))
                        .as("%s：gold 与查询同主题向量，kNN 单路应命中", c.caseId())
                        .isPositive();
                assertThat(fusedRrs.get(i))
                        .as("%s：kNN 腿应把 gold 拉回融合 top5", c.caseId())
                        .isPositive();
            }

            Long recorded = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM eo_retrieval_metric WHERE run_id = ?", Long.class, runId);
            assertThat(recorded)
                    .as("评测分量应逐条落 eo_retrieval_metric（ADR-0012 承诺的参数调整回看数据源）")
                    .isEqualTo((long) cases.size());
        } finally {
            docs.forEach(d -> elasticsearchOperations.delete(d.id(), ProductDocument.class));
            elasticsearchOperations.indexOps(ProductDocument.class).refresh();
        }
    }

    private void logAblationTable(List<EvalCase> cases, List<Double> knn, List<Double> bm25, List<Double> fused) {
        var table = new StringBuilder(
                "%n[asset-retrieval-ablation] 三腿消融对照（%d 用例 × topK=%d，合成向量语料）%n".formatted(cases.size(), TOP_K));
        table.append("  leg        | hit@5 | MRR%n".formatted());
        table.append("  kNN-only   | %.2f  | %.4f%n".formatted(hitRate(knn), mrr(knn)));
        table.append("  BM25-only  | %.2f  | %.4f%n".formatted(hitRate(bm25), mrr(bm25)));
        table.append("  RRF fused  | %.2f  | %.4f%n".formatted(hitRate(fused), mrr(fused)));
        for (int i = 0; i < cases.size(); i++) {
            var c = cases.get(i);
            table.append("  %-26s rr: kNN=%.2f BM25=%.2f fused=%.2f%n"
                    .formatted(c.caseId(), knn.get(i), bm25.get(i), fused.get(i)));
        }
        log.info("{}", table);
    }

    /**
     * 评测用例。必挂组：gold 标题/描述与查询零词面重叠（IK 切词后无公共 token，2 字 CJK 词 fuzziness AUTO
     * 不产生模糊匹配）→ BM25 单路只能命中特意配的「查询词干扰文档」。混合组：词面指向一个主题、向量指向另一个，
     * 两腿各出一名，考融合的并排能力。
     */
    private static List<EvalCase> cases() {
        return List.of(
                new EvalCase("asset-bm25-miss-battery", "电池耐用 特别省电", 100, "it-asset-01", true),
                new EvalCase("asset-bm25-miss-camera", "夜晚拍娃清楚", 120, "it-asset-03", true),
                new EvalCase("asset-bm25-miss-keyboard", "打游戏敲字舒服的装备", 140, "it-asset-05", true),
                new EvalCase("asset-lexical-earbuds", "QuarkPods 曜石黑", 160, "it-asset-07", false),
                new EvalCase("asset-lexical-tablet", "SlatePad 银川蓝", 170, "it-asset-08", false),
                new EvalCase("asset-lexical-band", "AeroBand 石墨灰", 200, "it-asset-11", false),
                new EvalCase("asset-mixed-astrocare-blue", "AstroCare 蓝色 手表", 190, "it-asset-09", false),
                new EvalCase("asset-mixed-astrocare-black", "AstroCare 黑色 手表", 180, "it-asset-10", false));
    }

    /**
     * 评测语料：必挂组的干扰文档（02/04/06）词面刻意覆盖对应查询词，制造「BM25 自信地答错」；
     * 09/10 同品牌异主题，供混合组互相充当对方主题的向量锚点。主题槽位间隔 10，one-hot 互相正交。
     */
    private static List<CorpusDoc> corpus() {
        return List.of(
                new CorpusDoc("it-asset-01", MARKER + " 拂晓典藏 直板备用机", "夜航黑 单卡超长待机", 100),
                new CorpusDoc("it-asset-02", MARKER + " 省电霸主 塞满三块电池", "电池容量大 超长续航", 110),
                new CorpusDoc("it-asset-03", MARKER + " 全画幅夜视微单机身", "星空摄影 高感光度", 120),
                new CorpusDoc("it-asset-04", MARKER + " 拍娃神器 支架套装", "夜晚补光 三脚架", 130),
                new CorpusDoc("it-asset-05", MARKER + " 机械键轴 客制化键盘", "热插拔 灯效", 140),
                new CorpusDoc("it-asset-06", MARKER + " 打游戏装备 人体工学键帽", "敲字舒服 长时间码字", 150),
                new CorpusDoc("it-asset-07", MARKER + " QuarkPods 降噪耳机 曜石黑", "主动降噪 头戴式", 160),
                new CorpusDoc("it-asset-08", MARKER + " SlatePad 平板 银川蓝", "大屏网课 手写笔", 170),
                new CorpusDoc("it-asset-09", MARKER + " AstroCare 儿童手表 蓝色", "防水 定位", 180),
                new CorpusDoc("it-asset-10", MARKER + " AstroCare 老人手表 黑色", "心率 跌倒提醒", 190),
                new CorpusDoc("it-asset-11", MARKER + " AeroBand 手环 石墨灰", "睡眠监测 心率带", 200));
    }

    // ── 消融单腿：查询构造与 AssetElasticsearchAdapter 逐参对称（candidateK=topK×2、num_candidates=100、
    // status=ONLINE 预过滤），改动任一侧都要同步另一侧，否则消融对照失去「同except单腿」的口径 ──

    private List<String> knnLegIds(List<Float> vector) {
        var query = NativeQuery.builder()
                .withPageable(PageRequest.of(0, TOP_K))
                .withKnnSearches(knn -> knn.field("nameEmbedding")
                        .queryVector(vector)
                        .k(TOP_K)
                        .numCandidates(NUM_CANDIDATES)
                        .filter(Queries.wrapperQueryAsQuery(ONLINE_ONLY_FILTER)))
                .withSort(byScoreDesc())
                .build();
        return searchIds(query);
    }

    private List<String> bm25LegIds(String queryText) {
        var query = NativeQuery.builder()
                .withPageable(PageRequest.of(0, TOP_K))
                .withQuery(Query.of(q -> q.bool(b -> b.must(m -> m.multiMatch(mm -> mm.query(queryText)
                                .type(TextQueryType.BestFields)
                                .fuzziness("AUTO")
                                .fields("name^3", "description")))
                        .filter(f -> f.term(t -> t.field("status").value("ONLINE"))))))
                .withSort(byScoreDesc())
                .build();
        return searchIds(query);
    }

    private List<String> searchIds(NativeQuery query) {
        return elasticsearchOperations.search(query, ProductDocument.class).getSearchHits().stream()
                .map(SearchHit::getId)
                .toList();
    }

    private static List<SortOptions> byScoreDesc() {
        return List.of(SortOptions.of(so -> so.score(s -> s.order(SortOrder.Desc))));
    }

    // ── 语料写入与指标口径 ──

    private void save(CorpusDoc doc) {
        elasticsearchOperations.save(ProductDocument.builder()
                .id(doc.id())
                .userId("u-it-eval")
                .name(doc.name())
                .description(doc.description())
                .categoryId("99")
                .categoryName("评测语料")
                .price(999.0)
                .conditionLevel("2")
                .status("ONLINE")
                .stock(1)
                .viewCount(0)
                .createTime(System.currentTimeMillis())
                .updateTime(System.currentTimeMillis())
                .nameEmbedding(topicVector(doc.topicDim()))
                .build());
    }

    /** one-hot 主题向量：gold 与查询同槽位 → 余弦 1.0 恒第一名，异主题 → 0.0，kNN 名次先验可测。 */
    private static List<Float> topicVector(int dim) {
        float[] v = new float[EMBEDDING_DIMS];
        v[dim] = 1f;
        var boxed = new ArrayList<Float>(EMBEDDING_DIMS);
        for (float x : v) {
            boxed.add(x);
        }
        return boxed;
    }

    private long countByMarker() {
        var query = NativeQuery.builder()
                .withQuery(Queries.wrapperQueryAsQuery(
                        "{\"wildcard\":{\"name.keyword\":{\"value\":\"" + MARKER + "*\"}}}"))
                .build();
        return elasticsearchOperations.count(query, ProductDocument.class);
    }

    /** MRR 分量：gold 在第 i 位（1 起）得 1/i，未命中为 0（与 GoldenSetEvaluator 同口径）。 */
    private static double reciprocalRank(String goldId, List<String> rankedIds) {
        for (int i = 0; i < rankedIds.size(); i++) {
            if (goldId.equals(rankedIds.get(i))) {
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
