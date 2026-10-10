package com.cartethyia.easyorange.ai.application.toolcall;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * 一次工具调用决策 — 解析视图（{@link ToolCallArguments}）+ 原生 tool call 的工具名与 arguments 原始串；
 * 产出方与消费方（编排器 / 内核）共用，故为公开 record。
 * <p>
 * 两个串在构造期归一化成非空：{@code ToolCall} 是裸 record，访问器在类型系统里非空却不校验 null，供应商回
 * {@code {"name": null}} 时 Jackson 照样绑进来；收敛成空串而非把 {@code @Nullable} 往下传，下游的 switch
 * 分发、回调表查名、trace 落库不必各写判空，空名也不会在某环被当成合法工具名继续流下去。
 *
 * @param rawArguments arguments JSON 原始串：模型原样产出、不重写
 */
public record ToolCallDecision(
        ToolCallArguments parsedArguments, String tool, String rawArguments, AssistantMessage.ToolCall rawToolCall) {

    /** 收敛轮工具名 — 两条工具面共享的协议词汇（与 {@link ToolLoopKernel#FINISH_TOOL} 同字面）。 */
    private static final String FINISH_TOOL = "finish";

    public static ToolCallDecision of(ToolCallArguments parsedArguments, AssistantMessage.ToolCall toolCall) {
        return new ToolCallDecision(parsedArguments, orEmpty(toolCall.name()), orEmpty(toolCall.arguments()), toolCall);
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    public boolean isFinish() {
        return FINISH_TOOL.equals(tool);
    }
}
