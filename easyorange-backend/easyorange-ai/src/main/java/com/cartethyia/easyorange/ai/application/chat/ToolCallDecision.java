package com.cartethyia.easyorange.ai.application.chat;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * 一次工具调用决策 — 解析出的参数视图 + 原生 tool call 的工具名与 arguments 原始串，一个来源。
 * <p>
 * 顶层而非嵌在编排器里：产出方是 {@link ToolCallDecider}、消费方是 {@link ToolCallLoop}，两边都要引用，
 * 嵌在编排器里就得为它放宽可见性。同包顶层是能让双方都引用的最小可见范围。
 * <p>
 * 两个串在构造期归一化成非空：Spring AI 的 {@code chat.messages} 包标了 {@code @NullMarked}，
 * 其 {@code ToolCall} record 的访问器在类型系统里是非空 {@code String}，但它是裸 record、不校验
 * null，供应商回 {@code {"name": null}} 时 Jackson 照样绑进来 —— 声明非空而运行期可空，这个缺口
 * 得我们自己收。收敛成空串而不是把 {@code @Nullable} 往下传，是为了让下游的 switch 分发、
 * 回调表查名、trace 落库都不必各写一次判空，也免得空名在某一环被当成「合法工具名」继续流下去。
 * 两个 arguments 分量显式区分 parsed / raw：解析视图与原始串是两个不同的东西，早年一个叫 {@code args}
 * 一个叫 {@code arguments}，只差四个字母，读到 {@code rawArguments()} 得先分辨它到底是哪一个。
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
