package com.cartethyia.easyorange.ai.application.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.testsupport.PropertyBindings;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatContextTrimmer (token 级上下文裁剪) -> 测试")
class ChatContextTrimmerTest {

    private SimpleMeterRegistry meterRegistry;
    private AiProperties aiProperties;
    private ChatContextTrimmer trimmer;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        aiProperties = PropertyBindings.bind(AiProperties.class);
        trimmer = new ChatContextTrimmer(aiProperties, meterRegistry);
    }

    /** 每条 n 个 ASCII 字符 ≈ n × 0.3 token 向上取整。 */
    private static ChatTurn asciiTurn(String role, int chars) {
        return new ChatTurn(role, "a".repeat(chars));
    }

    @Test
    @DisplayName("预算内 -> 原样返回,记 within 不记 trimmed")
    void trim_withinBudget() {
        List<ChatTurn> history = List.of(asciiTurn("user", 100), asciiTurn("assistant", 100));

        ChatContextTrimmer.TrimResult result = trimmer.trim(history);

        assertThat(result.history()).isSameAs(history);
        assertThat(result.trimmed()).isFalse();
        assertThat(meterRegistry
                        .counter("easyorange.ai.chat.context.trim", "action", "within")
                        .count())
                .isEqualTo(1.0);
        assertThat(meterRegistry
                        .counter("easyorange.ai.chat.context.trim", "action", "trimmed")
                        .count())
                .isZero();
    }

    @Test
    @DisplayName("超预算 -> 从最新向前保留连续窗口")
    void trim_keepsNewestContiguousWindow() {
        // 每条 400 字符 = 120 token；预算 300 -> 放得下最新 2 条（240），第 3 条（360）放不下
        List<ChatTurn> history = List.of(
                asciiTurn("user", 400),
                asciiTurn("assistant", 400),
                asciiTurn("user", 400),
                asciiTurn("assistant", 400));
        trimmer = new ChatContextTrimmer(
                PropertyBindings.bind(AiProperties.class, "chat.max-history-tokens", "300"), meterRegistry);

        ChatContextTrimmer.TrimResult result = trimmer.trim(history);

        assertThat(result.trimmed()).isTrue();
        assertThat(result.history()).containsExactly(history.get(2), history.get(3));
        assertThat(result.estimatedTokens()).isEqualTo(240);
        assertThat(meterRegistry
                        .counter("easyorange.ai.chat.context.trim", "action", "trimmed")
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("单条超预算 -> 至少保留最新一条,永不返回空")
    void trim_neverReturnsEmpty() {
        List<ChatTurn> history = List.of(asciiTurn("assistant", 10_000));
        trimmer = new ChatContextTrimmer(
                PropertyBindings.bind(AiProperties.class, "chat.max-history-tokens", "100"), meterRegistry);

        ChatContextTrimmer.TrimResult result = trimmer.trim(history);

        assertThat(result.history()).containsExactly(history.get(0));
        assertThat(result.trimmed()).isTrue();
    }

    @Test
    @DisplayName("预算 <=0 -> 关闭裁剪,不产口径")
    void trim_disabled() {
        List<ChatTurn> history = List.of(asciiTurn("user", 100), asciiTurn("assistant", 100));
        trimmer = new ChatContextTrimmer(
                PropertyBindings.bind(AiProperties.class, "chat.max-history-tokens", "0"), meterRegistry);

        ChatContextTrimmer.TrimResult result = trimmer.trim(history);

        assertThat(result.history()).isSameAs(history);
        assertThat(result.trimmed()).isFalse();
        assertThat(meterRegistry.getMeters()).isEmpty();
    }

    @Test
    @DisplayName("空历史 -> 预算内直通")
    void trim_emptyHistory() {
        ChatContextTrimmer.TrimResult result = trimmer.trim(List.of());

        assertThat(result.history()).isEmpty();
        assertThat(result.trimmed()).isFalse();
        assertThat(result.estimatedTokens()).isZero();
    }
}
