package com.cartethyia.easyorange.ai.application.support;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * 一次工具调用决策的内核视图 — 编排内核只消费这五个分量，工具参数并集按链路各自解析（chat 的六字段
 * 并集留在 chat 包，listing 只需要 query），不进内核。
 * <p>
 * thought 与 toolInput 由决策方在产出时抽取：二者都是 trace / SSE 的展示字段，内核拿原始 arguments
 * JSON 自己再解析一遍只会把「参数视图」的形状焊死进内核，工具面加参数就得改两处。
 *
 * @param tool        工具名（决策侧已归一化为非空）
 * @param thought     模型的本步理由（trace / SSE 展示用，可空）
 * @param toolInput   入参摘要（trace 展示用，按工具取对应分量；多数工具为 null）
 * @param rawArguments   arguments JSON 原始串：按名分发执行与协议回填都用它（模型原样产出，不重写）
 * @param rawToolCall    原生 tool call：回填时取 id 与 name，与观察逐条对应
 */
public record ToolLoopCall(
        String tool,
        @Nullable String thought,
        @Nullable String toolInput,
        String rawArguments,
        AssistantMessage.ToolCall rawToolCall) {}
