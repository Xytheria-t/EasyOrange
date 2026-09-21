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
    private static ChatTurn asciiTurn(ChatTurn.Role role, int chars) {
        return new ChatTurn(role, "a".repeat(chars));
    }

    private double trimCount(String action) {
        return meterRegistry
                .counter("easyorange.ai.chat.context.trim", "action", action)
                .count();
    }

    private double recordedTokens() {
        return meterRegistry.summary("easyorange.ai.chat.context.tokens").totalAmount();
    }

    @Test
    @DisplayName("预算内 -> 原样返回,记 within 不记 trimmed")
    void trim_withinBudget() {
        List<ChatTurn> history = List.of(asciiTurn(ChatTurn.Role.USER, 100), asciiTurn(ChatTurn.Role.ASSISTANT, 100));

        List<ChatTurn> kept = trimmer.trim(history);

        assertThat(kept).isSameAs(history);
        assertThat(trimCount("within")).isEqualTo(1.0);
        assertThat(trimCount("trimmed")).isZero();
        assertThat(recordedTokens()).isEqualTo(60);
    }

    @Test
    @DisplayName("超预算 -> 从最新向前保留连续窗口")
    void trim_keepsNewestContiguousWindow() {
        // 每条 400 字符 = 120 token；预算 300 -> 放得下最新 2 条（240），第 3 条（360）放不下
        List<ChatTurn> history = List.of(
                asciiTurn(ChatTurn.Role.USER, 400),
                asciiTurn(ChatTurn.Role.ASSISTANT, 400),
                asciiTurn(ChatTurn.Role.USER, 400),
                asciiTurn(ChatTurn.Role.ASSISTANT, 400));
        trimmer = new ChatContextTrimmer(
                PropertyBindings.bind(AiProperties.class, "chat.max-history-tokens", "300"), meterRegistry);

        List<ChatTurn> kept = trimmer.trim(history);

        assertThat(kept).containsExactly(history.get(2), history.get(3));
        assertThat(trimCount("trimmed")).isEqualTo(1.0);
        assertThat(recordedTokens()).isEqualTo(240);
    }

    @Test
    @DisplayName("单条超预算 -> 至少保留最新一条,永不返回空")
    void trim_neverReturnsEmpty() {
        List<ChatTurn> history = List.of(asciiTurn(ChatTurn.Role.ASSISTANT, 10_000));
        trimmer = new ChatContextTrimmer(
                PropertyBindings.bind(AiProperties.class, "chat.max-history-tokens", "100"), meterRegistry);

        List<ChatTurn> kept = trimmer.trim(history);

        assertThat(kept).containsExactly(history.get(0));
        assertThat(trimCount("trimmed")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("预算 <=0 -> 关闭裁剪,不产口径")
    void trim_disabled() {
        List<ChatTurn> history = List.of(asciiTurn(ChatTurn.Role.USER, 100), asciiTurn(ChatTurn.Role.ASSISTANT, 100));
        trimmer = new ChatContextTrimmer(
                PropertyBindings.bind(AiProperties.class, "chat.max-history-tokens", "0"), meterRegistry);

        List<ChatTurn> kept = trimmer.trim(history);

        assertThat(kept).isSameAs(history);
        assertThat(meterRegistry.getMeters()).isEmpty();
    }

    @Test
    @DisplayName("空历史 -> 预算内直通")
    void trim_emptyHistory() {
        List<ChatTurn> kept = trimmer.trim(List.of());

        assertThat(kept).isEmpty();
        assertThat(trimCount("within")).isEqualTo(1.0);
        assertThat(recordedTokens()).isZero();
    }
}
