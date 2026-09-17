package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.application.eval.EvalBaselines;
import com.cartethyia.easyorange.ai.application.eval.EvalGate;
import com.cartethyia.easyorange.ai.application.eval.GoldenSetEvaluator;
import com.cartethyia.easyorange.ai.application.eval.GoldenSetLoader;
import com.cartethyia.easyorange.ai.domain.model.GenerationReport;
import com.cartethyia.easyorange.ai.domain.model.RetrievalReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 金标准集回归门禁（评估进 CI）— 跑真实 LLM 对 golden-set.yaml 全部用例
 * Judge 打分 + 检索指标，分数低于「基线 - 容忍度」或评审覆盖率不达标即失败（卡 build）。
 * <p>
 * 需要真实 AI key：CI 的 ai-eval job 注入 EASYORANGE_AI_API_KEY 后经 failsafe 在 verify
 * 阶段执行；本地/无 key 时自动跳过（@EnabledIfEnvironmentVariable）。
 * <p>
 * <b>需要 ES</b>：retrieval 用例的 gold_doc_ids 按 ES 索引设计（dense_vector kNN + BM25 排名融合），
 * 种子文档以 status=PENDING 入库、由启动补索引（{@code KnowledgeBootstrapIndexer}）同步写入 ES。
 * 环境无 ES 时检索降级为 MySQL LIKE（只命中关键词字面重叠，召回质量差一个档次），
 * hit@5 必然明显下滑 —— 这是「测的是降级路径而非产品检索」的显式失败，不是抖动。
 * CI 由 ai-eval.yml 起 ES 并传 {@code -Deasyorange.search.elasticsearch.enabled=true}。
 */
@EnabledIfEnvironmentVariable(named = "EASYORANGE_AI_API_KEY", matches = ".+")
class GoldenSetRegressionIT extends AbstractIntegrationTest {

    @Autowired
    private GoldenSetLoader loader;

    @Autowired
    private GoldenSetEvaluator evaluator;

    @Test
    void generationScoreAboveBaseline() {
        GenerationReport report = evaluator.evaluateGeneration();
        EvalBaselines.Generation thresholds = loader.loadBaselines().generation();

        // 覆盖率先行：评审大面积失败时均分是「幸存者平均」，不能拿它当质量结论
        EvalGate.GateResult coverage = EvalGate.checkCoverage(
                GoldenSetLoader.SCOPE_CHAT, report.judgedCases(), report.totalCases(), thresholds.minCoverage());
        assertThat(coverage.passed())
                .as(
                        "chat 用例评审覆盖率 %.0f%%（%d/%d）低于下限 %.0f%%，均分不可信",
                        coverage.actual() * 100, report.judgedCases(), report.totalCases(), coverage.baseline() * 100)
                .isTrue();

        EvalGate.GateResult gate = EvalGate.check(
                GoldenSetLoader.SCOPE_CHAT, report.avgScore(), thresholds.scoreBaseline(), thresholds.scoreTolerance());
        assertThat(gate.passed())
                .as(
                        "chat 平均分 %.2f 低于基线 %.2f - %.2f（阈值见 eval/baselines.yaml），AI 质量发生回归",
                        report.avgScore(), thresholds.scoreBaseline(), thresholds.scoreTolerance())
                .isTrue();
    }

    @Test
    void retrievalMetricsCollected() {
        double minHitAt5 = loader.loadBaselines().retrieval().minHitAt5();
        RetrievalReport report = evaluator.evaluateRetrieval();

        assertThat(report.totalCases()).as("金标准集检索用例应非空").isGreaterThan(0);
        assertThat(report.hitRateAt5())
                .as("hit@5 低于下限 %.2f（阈值见 eval/baselines.yaml）：语料含同域干扰文档，命中不再是必然", minHitAt5)
                .isGreaterThanOrEqualTo(minHitAt5);
    }
}
