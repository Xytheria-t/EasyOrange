package com.cartethyia.easyorange.ai.domain.model;

/**
 * 工具循环结局事件 — SSE {@code outcome} 事件的载荷，前端据此在步骤面板标注「这一轮是降级收的」。
 * <p>
 * 传结局标签而非枚举：{@code ToolLoopOutcome} 的 tag 是显式固化的时序契约，标签留在 application 层，
 * domain 端口不反向依赖它（ArchUnit Rule 1）。
 *
 * @param outcome 结局标签：{@code finished} / {@code step_limit} / {@code budget} / {@code decision_failed}
 * @param rounds  实际跑了几轮（降级时即「跑到哪一轮被截断」）
 */
public record ToolLoopOutcomeView(String outcome, int rounds) {}
