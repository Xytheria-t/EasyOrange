package com.cartethyia.easyorange.ai.application.eval;

/**
 * 评估门禁 — 金标准集回归分数与基线对比，低于「基线 - 容忍度」即失败（卡 build）。
 * <p>
 * 与 JaCoCo/PIT 门禁同一思路：质量不是「感觉」，是可量化、可回归、可卡 CI 的数值。
 * <p>
 * 门禁检查两件事：<b>覆盖率</b>（有多少用例真的跑成功）与<b>分数</b>（成功的那些答得怎么样）。
 * 只卡分数会漏掉一整类回归：模型或评测链路大面积失败时，均分只统计幸存用例，
 * 极端情况下 1 条打 5 分就能让门禁变绿 —— 覆盖率先挡住这种情况。
 */
public final class EvalGate {

    /** 评审覆盖率下限：低于此比例说明「大部分用例没跑成功」，均分不再代表质量。 */
    public static final double DEFAULT_MIN_COVERAGE = 0.8;

    private EvalGate() {}

    public record GateResult(boolean passed, String scope, double actual, double baseline, double delta) {}

    /**
     * @param tolerance 允许低于基线的幅度（如 0.3 分）。A/B 改造后分数下滑超过容忍度即判失败。
     */
    public static GateResult check(double actual, double baseline, double tolerance) {
        double delta = actual - baseline;
        return new GateResult(delta >= -tolerance, "chat", actual, baseline, delta);
    }

    /**
     * 覆盖率门禁：判定「跑成功并计入均分」的用例占比是否达标。
     *
     * @param minRatio 覆盖率下限（{@link #DEFAULT_MIN_COVERAGE}）
     */
    public static GateResult checkCoverage(String scope, int judged, int total, double minRatio) {
        double ratio = total <= 0 ? 0 : (double) judged / total;
        return new GateResult(ratio >= minRatio, scope + "-coverage", ratio, minRatio, ratio - minRatio);
    }
}
