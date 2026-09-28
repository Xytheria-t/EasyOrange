package com.cartethyia.easyorange.ai.application.chat;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * 决策对话序列 — 首两条（system + 首条 user）每请求固定，每执行一步按「assistant tool_call +
 * role=tool 观察」逐轮回填，轮间前缀稳定命中供应商 KV cache 折扣。
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

    void appendStep(AssistantMessage.ToolCall toolCall, String observation) {
        messages.add(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(toolCall))
                .build());
        messages.add(ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), observation)))
                .build());
    }
}
