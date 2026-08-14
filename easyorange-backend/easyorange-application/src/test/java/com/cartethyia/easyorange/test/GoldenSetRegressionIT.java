package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.eval.EvalGate;
import com.cartethyia.easyorange.ai.eval.GenerationReport;
import com.cartethyia.easyorange.ai.eval.GoldenSetEvaluator;
import com.cartethyia.easyorange.ai.eval.GoldenSetLoader;
import com.cartethyia.easyorange.ai.eval.RetrievalReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 金标准集回归门禁（评估进 CI）— 跑真实 LLM（DeepSeek）对 golden-set.yaml 全部用例
 * Judge 打分 + 检索指标，分数低于「基线 - 容忍度」即失败（卡 build）。
 * <p>
 * 需要真实 AI key：CI 的 nightly job 注入 EASYORANGE_AI_API_KEY 后经 failsafe 在 verify
 * 阶段执行；本地/无 key 时自动跳过（@EnabledIfEnvironmentVariable）。检索指标用例依赖
 * 知识库种子文档已摄入 ES（启动补索引自动完成）。
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
        assertThat(report.judgedCases()).as("金标准集生成用例应全部完成评审").isGreaterThan(0);

        double baseline = loader.loadBaselines().getOrDefault("chat", 4.0);
        EvalGate.GateResult gate = EvalGate.check(report.avgScore(), baseline, 0.3);
        assertThat(gate.passed())
                .as("chat 平均分 %.2f 低于基线 %.2f - 0.3，AI 质量发生回归", report.avgScore(), baseline)
                .isTrue();
    }

    @Test
    void retrievalMetricsCollected() {
        RetrievalReport report = evaluator.evaluateRetrieval();
        assertThat(report.totalCases()).as("金标准集检索用例应非空").isGreaterThan(0);
        assertThat(report.hitRateAt5()).as("hit@5 至少命中一半（知识库种子文档齐全时）").isGreaterThanOrEqualTo(0.5);
    }
}
