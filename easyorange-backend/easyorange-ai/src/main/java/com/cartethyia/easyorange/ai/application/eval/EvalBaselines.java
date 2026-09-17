package com.cartethyia.easyorange.ai.application.eval;

/**
 * 评估门禁阈值（{@code eval/baselines.yaml}）—— 按 scope 分节，与文件结构一一对应。
 * <p>
 * 阈值放配置而不是测试代码里：调基线是「跑一次回归、评审、改 yaml」，不需要改 Java 重新编译；
 * 「门禁卡多严」也变成可评审的 diff，而不是散在断言里的魔数。
 * <p>
 * 缺失键在加载期抛异常（{@link GoldenSetLoader#loadBaselines()}），不给默认值 ——
 * 门禁阈值静默回落成内置默认值，等于门禁悄悄放松，比加载失败危险。
 *
 * @param generation 生成质量（{@code chat} 用例）门槛
 * @param retrieval  检索质量（{@code retrieval} 用例）门槛
 */
public record EvalBaselines(Generation generation, Retrieval retrieval) {

    /**
     * @param scoreBaseline  Judge 平均分基线（1-5）
     * @param scoreTolerance 允许低于基线的幅度；A/B 改造后分数下滑超过它即判失败
     * @param minCoverage    评审覆盖率下限：低于它说明「大部分用例没跑成功」，均分不可信
     */
    public record Generation(double scoreBaseline, double scoreTolerance, double minCoverage) {}

    /**
     * @param minHitAt5 hit@5 下限；语料与 topK 同量级时 hit@5 恒满分，该值才需要随语料扩容上调
     */
    public record Retrieval(double minHitAt5) {}
}
