package com.cartethyia.easyorange.ai.application.support;

/**
 * 循环观测副产物的记账口 — 内核只报事实（结局 / 轮数 / 工具耗时），指标名与 tag 契约归各链路的
 * 实现（chat 是 easyorange.ai.chat.*，listing 是 easyorange.ai.listing.*）：埋点属于链路而非编排。
 * <p>
 * 观测失败绝不影响主链路：实现方只收已算好的值，不抛异常、不反查配置。
 */
public interface ToolLoopListener {

    /** 记一次循环的结局与决策轮数 —— 结局含降级归因，轮数是「平均步数」口径的来源。 */
    void recordLoop(ToolCallLoopOutcome outcome, int rounds);

    /** 记一次故障穿透 — 只记结局，不进轮数分布（故障没有出口、轮数不可知，记 0 会把均值往 0 拽）。 */
    void recordLoopFailure();

    /** 记一次工具执行：调用计数 + 执行耗时（只含工具执行本身，不含本轮决策的模型调用）。 */
    void recordTool(String toolName, long latencyMs);
}
