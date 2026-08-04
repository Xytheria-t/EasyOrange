package com.cartethyia.easyorange.ai.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * AI 未配置时的占位 {@link ChatModel} — 对应文本/视觉模型 key 缺失时装配。
 * <p>
 * 调用即抛「AI 模型未配置」异常，由服务层现有 try/catch 降级为 null（fail-open），
 * 保证应用无需 AI key 即可启动，与 AGENTS.md「AI 密钥可选、不影响应用启动」契约一致。
 */
public class UnconfiguredChatModel implements ChatModel {

    private final String reason;

    public UnconfiguredChatModel(String reason) {
        this.reason = reason;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        throw new IllegalStateException("AI 模型未配置：" + reason);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        throw new IllegalStateException("AI 模型未配置：" + reason);
    }
}
