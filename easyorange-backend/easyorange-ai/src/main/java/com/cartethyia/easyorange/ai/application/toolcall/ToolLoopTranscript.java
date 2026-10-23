package com.cartethyia.easyorange.ai.application.toolcall;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * 工具循环的决策侧对话记录 —— 循环每轮要重发给模型的那串消息，随轮次增长：首两条（system + user）每请求固定，
 * 之后每轮追加「assistant tool_calls + role=tool 观察」。
 * <p>
 * 在链路里的位置：由 {@code ToolLoopKernel} 持有，每轮把 {@link #messages()} 全量交给决策函数 —— 本项目不做增量
 * 续传，靠「前缀跨轮逐字不变」命中供应商 KV cache 折扣。
 * <p>
 * 一轮的多个调用合成一条 assistant 消息（{@code tool_calls} 数组）+ 一条 tool 消息（多份响应）：协议要求每个
 * {@code tool_call_id} 都有对应观察，少一条供应商直接 400。
 */
public final class ToolLoopTranscript {

    private final List<Message> messages;

    public ToolLoopTranscript(String systemPrompt, String firstUserMessage) {
        this.messages = new ArrayList<>(2);
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(firstUserMessage));
    }

    /** 当前消息序列的不可变副本 —— 后续回填对它不可见。 */
    public List<Message> messages() {
        return List.copyOf(messages);
    }

    /** 回填一轮的全部调用与观察 —— 两侧同序且逐条对应，缺一条对应关系就是协议不合法。 */
    public void appendRound(List<ToolCallStep> calls, List<String> observations) {
        if (calls.size() != observations.size()) {
            throw new IllegalArgumentException("tool call 与观察数量不一致，无法按协议回填");
        }
        var responses = new ArrayList<ToolResponseMessage.ToolResponse>(calls.size());
        var rawCalls = new ArrayList<AssistantMessage.ToolCall>(calls.size());
        for (int i = 0; i < calls.size(); i++) {
            AssistantMessage.ToolCall raw = calls.get(i).rawToolCall();
            rawCalls.add(raw);
            responses.add(new ToolResponseMessage.ToolResponse(raw.id(), raw.name(), observations.get(i)));
        }
        messages.add(AssistantMessage.builder().content("").toolCalls(rawCalls).build());
        messages.add(ToolResponseMessage.builder().responses(responses).build());
    }
}
