package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.application.eval.GoldenSetEvaluator;
import com.cartethyia.easyorange.ai.domain.model.ArmComparisonReport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * RAG 有效性双臂对照 — 同批 chat 用例，唯一变量是检索来源（有检索 = 现状全量工具面；无检索 = 只挂 finish）。
 * <p>
 * 消费面是配对差分而非两臂绝对分：Judge 与被评模型同源，绝对分带自评偏差，对外不可引用；
 * 同模型自评偏差在两臂同向抵消，配对差可用（口径见 {@link ArmComparisonReport}）。
 * <p>
 * 门禁语义：本 IT <b>不断言差分符号</b>。RAG 在这批规则问答上到底帮不帮忙是结论不是门禁 ——
 * 若模型自身知识已覆盖平台规则，差分趋零甚至为负都是真实结论，钉死符号等于把结论写成断言、
 * 下次换模型就假红。只断言两臂都真的评上了分（差分可算的前提）。
 * <p>
 * 需要真实 AI key（同 GoldenSetRegressionIT）：22 chat 用例 × 2 臂 ×（决策轮 + 生成 + Judge）。
 */
@Slf4j
@EnabledIfEnvironmentVariable(named = "EASYORANGE_AI_API_KEY", matches = ".+")
class RagArmComparisonIT extends AbstractIntegrationTest {

    @Autowired
    private GoldenSetEvaluator evaluator;

    @Test
    @DisplayName("RAG 双臂对照：两臂配对差分（不钉差分符号，结论不作门禁）")
    void pairedDiffAcrossBothArms() {
        ArmComparisonReport report = evaluator.evaluateRagArmComparison();

        assertThat(report.pairedCases())
                .as("两臂都成功评分的用例数（配对差分的分母），流失明细：%s", report.missingArms())
                .isPositive();
        assertThat(report.missingArms()).as("两臂必须都评上分才能算配对差分，单臂缺评分说明有一臂跑挂了").isEmpty();

        log.info("""
                [rag-arm-comparison] RAG 有效性对照（配对 n=%d）
                  有检索臂均分 : %.2f
                  无检索臂均分 : %.2f
                  配对差分均值 : %+.4f  (标准差 %.4f)
                  差分标准误   : %.4f
                """.formatted(
                        report.pairedCases(),
                        report.retrievalMean(),
                        report.noRetrievalMean(),
                        report.meanDiff(),
                        report.diffStdDev(),
                        standardError(report)));
    }

    /** 差分均值的标准误 —— n=22 量级下单轮方差大，配对差要跟自身标准误比才谈得上显著。 */
    private static double standardError(ArmComparisonReport report) {
        return report.pairedCases() < 2 ? 0 : report.diffStdDev() / Math.sqrt(report.pairedCases());
    }
}
