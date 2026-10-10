package com.cartethyia.easyorange.ai.application.toolcall;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * 一次工具调用决策的内核视图 — 内核只消费这五个分量；参数并集（{@link ToolCallArguments}）由
 * {@link ToolCallDecider} 统一解析，链路只注入入参摘要抽取，解析视图不进内核。
 * <p>
 * thought 与 toolInput 由决策方在产出时抽取：二者是 trace / SSE 的展示字段，内核拿原始 arguments JSON
 * 自己再解析一遍会把「参数视图」的形状焊死进内核，工具面加参数就得改两处。
 *
 * @param tool        工具名（决策侧已归一化为非空）
 * @param thought     模型本步理由（trace / SSE 展示用，可空）
 * @param toolInput   入参摘要（trace 展示用；多数工具为 null）
 * @param rawArguments   arguments JSON 原始串：按名分发执行与协议回填都用它（不重写）
 * @param rawToolCall    原生 tool call：回填时取 id 与 name，与观察逐条对应
 */
public record ToolLoopCall(
        String tool,
        @Nullable String thought,
        @Nullable String toolInput,
        String rawArguments,
        AssistantMessage.ToolCall rawToolCall) {}
