package com.cartethyia.easyorange.ai.application.toolcall;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 工具调用循环（ReAct）的结局 — 封闭集合：一条正常收敛出口、三条降级出口、一条故障哨兵。
 * <p>
 * 三条降级出口都从自治循环切回确定性单次生成（已积累观察不丢弃），区别只在切断原因；哨兵不是出口，只接
 * 基础设施故障穿透，正常应为零。四条真实出口见 {@link ToolLoopKernel} 的 for 循环。
 * <p>
 * 纯观测口径（指标与降级归因），生成链路不按它分支；放共用包而非某个编排器，落任一能力包都会造成跨包
 * 反向依赖。
 */
@Getter
@RequiredArgsConstructor
public enum ToolLoopOutcome {
    FINISHED("finished"),

    ROUND_LIMIT("step_limit"),

    BUDGET("budget"),

    DECISION_FAILED("decision_failed"),

    /** 基础设施故障穿透的哨兵（非循环出口，正常应为零）。 */
    ERROR("error");

    /** 指标 tag（{@code easyorange.ai.chat.loop{outcome}}）— 显式固化，枚举改名不影响时序数据。 */
    private final String tag;
}
