package com.cartethyia.easyorange.ai.application.toolcall;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * 解析后的工具调用 — 模型回包里的原生 tool call，加上 arguments 的解析视图（{@link ToolCallArguments}）；由
 * {@link ToolCallDecider} 产出、链路的入参摘要抽取消费。不交内核：内核拿到的是收敛后的 {@link ToolCallStep}。
 * <p>
 * 两个串在构造期归一化成非空：{@code ToolCall} 是裸 record，访问器在类型系统里非空却不校验 null，供应商回
 * {@code {"name": null}} 时 Jackson 照样绑进来；收敛成空串而非把 {@code @Nullable} 往下传，下游的 switch
 * 分发、回调表查名、trace 落库不必各写判空，空名也不会在某环被当成合法工具名继续流下去。
 *
 * @param rawArguments arguments JSON 原始串：模型原样产出、不重写
 */
public record ParsedToolCall(
        ToolCallArguments parsedArguments, String tool, String rawArguments, AssistantMessage.ToolCall rawToolCall) {

    public static ParsedToolCall of(ToolCallArguments parsedArguments, AssistantMessage.ToolCall toolCall) {
        return new ParsedToolCall(parsedArguments, orEmpty(toolCall.name()), orEmpty(toolCall.arguments()), toolCall);
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }
}
