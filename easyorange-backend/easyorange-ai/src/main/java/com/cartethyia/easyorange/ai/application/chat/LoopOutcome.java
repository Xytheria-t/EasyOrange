package com.cartethyia.easyorange.ai.application.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Agent 工具循环（ReAct）的结局 — 封闭集合：一条正常收敛出口、三条降级出口、一条故障哨兵。
 * <p>
 * 四条真实出口都在 {@code AgentLoopRunner} 的 for 循环上（finish / 步数超限 / 预算耗尽 / 决策失败），
 * 其中三条降级出口都把自治循环切回确定性单次生成（已积累观察不丢弃），区别只在切断原因。
 * {@link #ERROR} 不是循环出口：循环内的决策 / 工具失败都已收敛成降级，能落到它头上的是
 * 基础设施故障穿透，正常应为零 —— 收进同一枚举只为共用同一套结局计数。
 * <p>
 * 结局是纯观测口径（指标与降级归因），生成链路不按它分支。独立成文件而非私有嵌进某个埋点类：
 * 产出方（{@link AgentLoopRunner} 的循环出口）与消费方（{@link AgentLoopMetrics} 的计数器）
 * 分属两个类，私有嵌套无处安放 —— 仓库里其余指标 tag 集能私有嵌套，正因为它们的生产者与
 * 消费者本就是同一个类。同包顶层是能让双方都引用的最小可见范围；领域层同样不收：没有一行
 * 领域代码读它，只为计数存在的词汇不进领域。
 */
@Getter
@RequiredArgsConstructor
public enum LoopOutcome {
    /** 模型判定信息足够（finish 轮），正常收敛。 */
    FINISHED("finished"),

    /** 步数上限内未 finish。 */
    STEP_LIMIT("step_limit"),

    /** 循环中途日预算余量不足。 */
    BUDGET("budget"),

    /** 决策调用故障 / 未返回工具调用 / 参数不可解析 — 按原始问题补一次检索。 */
    DECISION_FAILED("decision_failed"),

    /** 基础设施故障穿透的哨兵（非循环出口，正常应为零；轮数不可知，故不进步数分布）。 */
    ERROR("error");

    /** 指标 tag 值（{@code easyorange.ai.chat.loop{outcome}}）— 显式固化，枚举改名不影响时序数据。 */
    private final String tag;
}
