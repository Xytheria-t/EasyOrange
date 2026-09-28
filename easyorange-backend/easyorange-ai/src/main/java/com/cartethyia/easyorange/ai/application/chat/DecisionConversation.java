package com.cartethyia.easyorange.ai.application.chat;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * 决策对话序列 — 首两条（system + 首条 user）每请求固定，每执行一轮按「assistant tool_calls +
 * role=tool 观察」逐轮回填，轮间前缀稳定命中供应商 KV cache 折扣。
 * <p>
 * 一轮内的多个工具调用合成一条 assistant 消息（{@code tool_calls} 数组）+ 一条 tool 消息（多条响应），
 * 与逐个调用时的消息形状同构：协议要求每个 {@code tool_call_id} 都有一条对应观察，少一条供应商直接 400。
 * <p>
 * 与生成路径的消息装配（{@link ChatPromptAssembler}）是一对对称职责：那边按角色装配单次生成的
 * prompt，这边逐轮累积决策上下文。两边都刻意不改写前缀内容（不改写历史、不压进单条 user 消息），
 * 因为前缀每变一个字节这轮的缓存折扣就全部作废。
 */
final class DecisionConversation {

    private final List<Message> messages;

    DecisionConversation(String systemPrompt, String firstUserMessage) {
        this.messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(firstUserMessage));
    }

    /** 当轮的不可变消息序列（循环后续追加对已发出的调用不可见）。 */
    List<Message> snapshot() {
        return List.copyOf(messages);
    }

    /**
     * 回填一轮的全部调用与观察 — 两侧同序且逐条对应，缺一条对应关系就是协议不合法。
     *
     * @param observations 与 {@code toolCalls} 一一对应的观察文本，长度必须相同
     */
    void appendRound(List<AssistantMessage.ToolCall> toolCalls, List<String> observations) {
        if (toolCalls.size() != observations.size()) {
            throw new IllegalArgumentException("tool call 与观察数量不一致，无法按协议回填");
        }
        var responses = new ArrayList<ToolResponseMessage.ToolResponse>(toolCalls.size());
        for (int i = 0; i < toolCalls.size(); i++) {
            AssistantMessage.ToolCall toolCall = toolCalls.get(i);
            responses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), observations.get(i)));
        }
        messages.add(AssistantMessage.builder()
                .content("")
                .toolCalls(List.copyOf(toolCalls))
                .build());
        messages.add(ToolResponseMessage.builder().responses(responses).build());
    }
}
