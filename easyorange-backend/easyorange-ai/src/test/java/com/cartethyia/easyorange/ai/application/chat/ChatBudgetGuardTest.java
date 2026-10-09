package com.cartethyia.easyorange.ai.application.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.dto.ChatRequest;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort;
import com.cartethyia.easyorange.ai.testsupport.PropertyBindings;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatBudgetGuard (对话日预算前置判定) -> 测试")
class ChatBudgetGuardTest {

    @Mock
    private TokenBudgetStorePort budgetStore;

    @Test
    @DisplayName("used + maxPerCall 越过日限即 true，未越即 false（循环中途只读检查与预留共用同一判据）")
    void exhausted_acrossDailyLimit() {
        when(budgetStore.getTodayUsage("chat")).thenReturn(Optional.of(usage(299_000)));
        assertThat(guard(PropertyBindings.bind(AiProperties.class)).exhausted()).isTrue();

        when(budgetStore.getTodayUsage("chat")).thenReturn(Optional.of(usage(1000)));
        assertThat(guard(PropertyBindings.bind(AiProperties.class)).exhausted()).isFalse();
    }

    @Test
    @DisplayName("场景配置优先于兜底常量：yaml 把日限调到 1 万，用量 2 万即超限")
    void exhausted_configOverridesFallback() {
        var properties = PropertyBindings.bind(
                AiProperties.class,
                "budget.scenarios.chat.max-tokens-per-call",
                "1500",
                "budget.scenarios.chat.daily-token-limit",
                "10000");
        when(budgetStore.getTodayUsage("chat")).thenReturn(Optional.of(usage(20_000)));

        assertThat(guard(properties).exhausted()).isTrue();
    }

    @Test
    @DisplayName("场景配置缺失 -> 走与 @TokenBudget 注解同值的兜底（不静默当作不限额）")
    void exhausted_fallsBackWhenScenarioMissing() {
        when(budgetStore.getTodayUsage("chat")).thenReturn(Optional.of(usage(299_000)));

        assertThat(guard(PropertyBindings.bind(AiProperties.class)).exhausted()).isTrue();
    }

    @Test
    @DisplayName("日限配 0 = 不限，任何用量都不判超限")
    void exhausted_zeroDailyLimitNeverExhausts() {
        var properties = PropertyBindings.bind(AiProperties.class, "budget.scenarios.chat.daily-token-limit", "0");
        when(budgetStore.getTodayUsage("chat")).thenReturn(Optional.of(usage(9_999_999)));

        assertThat(guard(properties).exhausted()).isFalse();
    }

    @Test
    @DisplayName("tryAcquire 余量充足返回句柄，耗尽返回 null（流式入口与切面同一条原子预留协议）")
    void tryAcquire_grantsOrDeniesAtomically() {
        when(budgetStore.tryReserve("chat", 1500, 300_000))
                .thenReturn(null)
                .thenReturn(TokenBudgetStorePort.TokenReservation.NOOP);

        var guard = guard(PropertyBindings.bind(AiProperties.class));

        assertThat(guard.tryAcquire()).isNull();
        assertThat(guard.tryAcquire()).isNotNull();
    }

    @Test
    @DisplayName("tryAcquire 的预留量与判据同源：场景配置缺失用注解同值的兜底常量")
    void tryAcquire_usesResolvedConfig() {
        var properties = PropertyBindings.bind(
                AiProperties.class,
                "budget.scenarios.chat.max-tokens-per-call",
                "800",
                "budget.scenarios.chat.daily-token-limit",
                "5000");
        when(budgetStore.tryReserve("chat", 800, 5000)).thenReturn(TokenBudgetStorePort.TokenReservation.NOOP);

        assertThat(guard(properties).tryAcquire()).isNotNull();
    }

    /**
     * 契约测试：兜底常量与 {@code @TokenBudget(scenario="chat")} 注解默认值必须一致。
     * 两者分属「手动预检」与「AOP 切面」两条路径，靠反射钉住 —— 改一边不改另一边直接红。
     */
    @Test
    @DisplayName("兜底常量 == @TokenBudget(scenario=chat) 注解默认值（改一边必须改另一边）")
    void fallbackMatchesAnnotationContract() throws NoSuchMethodException {
        TokenBudget limits = AiChatAppService.class
                .getDeclaredMethod("answer", ChatRequest.class, String.class)
                .getAnnotation(TokenBudget.class);

        assertThat(limits).isNotNull();
        assertThat(limits.scenario()).isEqualTo("chat");
        assertThat(ChatBudgetGuard.DEFAULT_MAX_TOKENS_PER_CALL).isEqualTo(limits.maxTokensPerCall());
        assertThat(ChatBudgetGuard.DEFAULT_DAILY_LIMIT).isEqualTo(limits.dailyTokenLimit());
    }

    private ChatBudgetGuard guard(AiProperties properties) {
        return new ChatBudgetGuard(budgetStore, properties);
    }

    private TokenBudgetStorePort.TokenUsage usage(int total) {
        return new TokenBudgetStorePort.TokenUsage(total, 0, 0, 0L);
    }
}
