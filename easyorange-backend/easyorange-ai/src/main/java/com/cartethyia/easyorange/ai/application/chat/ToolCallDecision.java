package com.cartethyia.easyorange.ai.application.chat;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * 一次工具调用决策 — 参数解析视图 + 原生 tool call 的工具名与 arguments 原始串，一个来源。
 * <p>
 * 顶层而非嵌在编排器里：产出方 {@link ToolCallDecider}、消费方 {@link ToolCallLoop} 都要引用，
 * 同包顶层是双方都能引用的最小可见范围。
 * <p>
 * 两个串在构造期归一化成非空：{@code chat.messages} 标了 {@code @NullMarked}，但 {@code ToolCall} 是裸
 * record、访问器在类型系统里是非空 {@code String} 却不校验 null，供应商回 {@code {"name": null}} 时
 * Jackson 照样绑进来。收敛成空串而非把 {@code @Nullable} 往下传，下游的 switch 分发、回调表查名、trace
 * 落库就不必各写一次判空，空名也不会在某一环被当成合法工具名继续流下去。
 *
 * @param parsedArguments arguments JSON 的解析视图（{@link ToolCallArguments}）：工具入参与本步理由的来源
 * @param rawArguments    arguments JSON 原始串：回调执行与协议回填都按它走（模型原样产出，不重写）
 */
record ToolCallDecision(
        ToolCallArguments parsedArguments, String tool, String rawArguments, AssistantMessage.ToolCall rawToolCall) {

    static ToolCallDecision of(ToolCallArguments parsedArguments, AssistantMessage.ToolCall toolCall) {
        return new ToolCallDecision(parsedArguments, orEmpty(toolCall.name()), orEmpty(toolCall.arguments()), toolCall);
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    boolean isFinish() {
        return ChatTools.TOOL_FINISH.equals(tool);
    }
}
