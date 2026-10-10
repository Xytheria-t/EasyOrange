package com.cartethyia.easyorange.ai.adapter.outbound.llm;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * AI 未配置时的占位 {@link ChatModel} — 文本/视觉模型 key 缺失时装配。
 * <p>
 * 调用即抛「AI 模型未配置」异常，由服务层现有 try/catch 降级为 null（fail-open）：
 * 保证无需任何 AI key 应用也能启动。
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

    /** 流式调用返回错误流而不是直接抛：声明 {@code Flux} 的方法里同步抛异常，会让 {@code onErrorResume}
     * 之类的操作符链完全失效（调用方以为挂了错误处理，实际根本没接上）。 */
    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.error(new IllegalStateException("AI 模型未配置：" + reason));
    }
}
