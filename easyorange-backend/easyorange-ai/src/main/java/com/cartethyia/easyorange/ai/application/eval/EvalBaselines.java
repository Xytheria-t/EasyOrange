package com.cartethyia.easyorange.ai.application.eval;

/** 评估门禁阈值（{@code eval/baselines.yaml}）— 按评估维度分节，与文件结构一一对应；放配置而非测试代码，是让调基线成为可评审的 diff。
 * 缺失键在加载期抛异常（{@link GoldenSetLoader#loadBaselines()}）不给默认值 —— 静默回落成内置默认等于门禁悄悄放松。 */
public record EvalBaselines(Generation generation, Retrieval retrieval, Routing routing) {

    /** 生成质量门槛（走 chat 用例）：scoreBaseline 是 Judge 平均分基线（1-5），scoreTolerance 是允许低于基线的幅度（超出即判失败），minCoverage 是评审覆盖率下限 —— 低于它说明大部分用例没跑成功、均分不可信。 */
    public record Generation(double scoreBaseline, double scoreTolerance, double minCoverage) {}

    /** hit@5 下限；语料与 topK 同量级时 hit@5 恒满分，该值才需要随语料扩容上调。 */
    public record Retrieval(double minHitAt5) {}

    /** 路由准确率下限（chat 单步用例与 listing 多步用例分开设：难度不同型，混一个分母会互相稀释）。 */
    public record Routing(double minAccuracy, double listingMinAccuracy) {}
}
