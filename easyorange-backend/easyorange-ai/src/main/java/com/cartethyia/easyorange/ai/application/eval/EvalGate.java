package com.cartethyia.easyorange.ai.application.eval;

/**
 * 评估门禁 — 金标准集回归分数与基线对比，低于「基线 - 容忍度」即失败（卡 build）。
 * <p>
 * 与 JaCoCo/PIT 门禁同一思路：质量不是「感觉」，是可量化、可回归、可卡 CI 的数值。
 * <p>
 * 门禁检查两件事：<b>覆盖率</b>（有多少用例真的跑成功）与<b>分数</b>（成功的那些答得怎么样）。
 * 只卡分数会漏掉一整类回归：模型或评测链路大面积失败时，均分只统计幸存用例，
 * 极端情况下 1 条打 5 分就能让门禁变绿 —— 覆盖率先挡住这种情况。
 * <p>
 * 本类只做判定，不含阈值：基线、容忍度、覆盖率下限一律来自 {@code eval/baselines.yaml}
 * （见 {@link EvalBaselines}），调用方传入，改阈值不用改这里。
 */
public final class EvalGate {

    private EvalGate() {}

    public record GateResult(boolean passed, String scope, double actual, double baseline, double delta) {}

    /**
     * @param scope     用例 scope（{@code chat} / {@code retrieval}），只用于结果标注与日志
     * @param tolerance 允许低于基线的幅度（如 0.3 分）。A/B 改造后分数下滑超过容忍度即判失败
     */
    public static GateResult check(String scope, double actual, double baseline, double tolerance) {
        double delta = actual - baseline;
        return new GateResult(delta >= -tolerance, scope, actual, baseline, delta);
    }

    /**
     * 覆盖率门禁：判定「跑成功并计入均分」的用例占比是否达标。
     *
     * @param minRatio 覆盖率下限（{@link EvalBaselines.Generation#minCoverage()}）
     */
    public static GateResult checkCoverage(String scope, int judged, int total, double minRatio) {
        double ratio = total <= 0 ? 0 : (double) judged / total;
        return new GateResult(ratio >= minRatio, scope + "-coverage", ratio, minRatio, ratio - minRatio);
    }
}
