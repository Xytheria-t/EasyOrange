package com.cartethyia.easyorange.ai.application.toolcall;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * 循环里的一步（step）— 一次工具调用：模型这一轮决定「调哪个工具、传什么参数」，内核拿它分发执行，再把观察回填
 * 给模型继续下一轮。一轮可有多条（模型并行发起）；{@code finish} 也走这个形态，只是不执行、直接收敛。
 * <p>
 * 由 {@link ToolCallDecider} 从 {@link ParsedToolCall} 收敛而来：参数解析视图（{@link ToolCallArguments}）留在
 * 链路侧，内核只认这五个分量 —— tool / rawArguments 用于按名分发，rawToolCall 用于协议回填（id 与观察逐条对应），
 * thought / toolInput 用于 trace 与 SSE 展示。展示字段在决策时抽一次：让内核为展示再解析一遍 arguments，等于把
 * 参数形状焊进循环，工具面加参数就得改两处。
 *
 * @param tool         工具名（决策侧已归一化为非空）
 * @param thought      模型本步理由（trace / SSE 展示用，可空）
 * @param toolInput    入参摘要（trace 展示用；多数工具为 null）
 * @param rawArguments arguments JSON 原始串：按名分发执行与协议回填都用它（不重写）
 * @param rawToolCall  原生 tool call：回填时取 id 与 name，与观察逐条对应
 */
public record ToolCallStep(
        String tool,
        @Nullable String thought,
        @Nullable String toolInput,
        String rawArguments,
        AssistantMessage.ToolCall rawToolCall) {}
