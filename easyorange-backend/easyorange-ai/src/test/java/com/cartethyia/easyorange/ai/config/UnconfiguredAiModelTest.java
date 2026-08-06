package com.cartethyia.easyorange.ai.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;

/**
 * AI 未配置占位模型单元测试 — 调用即抛明确异常，由服务层 catch 降级为 null（fail-open）。
 */
@DisplayName("AI 未配置占位模型")
class UnconfiguredAiModelTest {

    private static final String REASON = "easyorange.ai.deepseek.api-key 为空";

    @Test
    @DisplayName("chatModel.call 抛出含配置原因的异常")
    void chatModel_call_throwsWithReason() {
        var model = new UnconfiguredChatModel(REASON);

        assertThatThrownBy(() -> model.call(new Prompt("hello")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(REASON);
    }

    @Test
    @DisplayName("chatModel.stream 抛出含配置原因的异常")
    void chatModel_stream_throwsWithReason() {
        var model = new UnconfiguredChatModel(REASON);

        assertThatThrownBy(() -> model.stream(new Prompt("hello")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(REASON);
    }

    @Test
    @DisplayName("embeddingModel.call 抛出含配置原因的异常")
    void embeddingModel_call_throwsWithReason() {
        var model = new UnconfiguredEmbeddingModel(REASON);

        assertThatThrownBy(() -> model.call(new EmbeddingRequest(
                        List.of("hello"), EmbeddingOptions.builder().build())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(REASON);
    }

    @Test
    @DisplayName("embeddingModel.embed(Document) 抛出含配置原因的异常")
    void embeddingModel_embed_throwsWithReason() {
        var model = new UnconfiguredEmbeddingModel(REASON);

        assertThatThrownBy(() -> model.embed(new Document("hello")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(REASON);
    }
}
