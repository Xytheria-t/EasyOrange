package com.cartethyia.easyorange.ai.application.toolcall;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 工具调用循环（ReAct）的结局 — 封闭集合：一条正常收敛出口、三条降级出口、一条故障哨兵。
 * <p>
 * 四条真实出口都在 {@link ToolLoopKernel} 的 for 循环上（finish / 步数超限 / 预算耗尽 / 决策失败），三条
 * 降级出口都把自治循环切回确定性单次生成（已积累观察不丢弃），区别只在切断原因；哨兵值不是出口，
 * 循环内的失败早已收敛成降级，它只接基础设施故障穿透，正常应为零。
 * <p>
 * 纯观测口径（指标与降级归因），生成链路不按它分支。放 support 而非某个编排器：chat 与 listing 两条链路
 * 共用同一份内核与结局词汇，落在任一能力包都会造成跨包反向依赖。
 */
@Getter
@RequiredArgsConstructor
public enum ToolCallLoopOutcome {
    FINISHED("finished"),

    STEP_LIMIT("step_limit"),

    BUDGET("budget"),

    DECISION_FAILED("decision_failed"),

    /** 基础设施故障穿透的哨兵（非循环出口，正常应为零；轮数不可知，故不进步数分布）。 */
    ERROR("error");

    /** 指标 tag 值（{@code easyorange.ai.chat.loop{outcome}}）— 显式固化，枚举改名不影响时序数据。 */
    private final String tag;
}
