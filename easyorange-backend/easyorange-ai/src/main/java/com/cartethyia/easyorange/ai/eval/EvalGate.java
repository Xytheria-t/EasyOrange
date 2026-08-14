package com.cartethyia.easyorange.ai.eval;

/**
 * 评估门禁 — 金标准集回归分数与基线对比，低于「基线 - 容忍度」即失败（卡 build）。
 * <p>
 * 与 JaCoCo/PIT 门禁同一思路：质量不是「感觉」，是可量化、可回归、可卡 CI 的数值。
 */
public final class EvalGate {

    private EvalGate() {}

    public record GateResult(boolean passed, String scope, double actual, double baseline, double delta) {}

    /**
     * @param tolerance 允许低于基线的幅度（如 0.3 分）。A/B 改造后分数下滑超过容忍度即判失败。
     */
    public static GateResult check(double actual, double baseline, double tolerance) {
        double delta = actual - baseline;
        return new GateResult(delta >= -tolerance, "chat", actual, baseline, delta);
    }
}
