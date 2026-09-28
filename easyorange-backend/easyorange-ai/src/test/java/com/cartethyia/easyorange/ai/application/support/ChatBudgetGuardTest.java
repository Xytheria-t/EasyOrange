package com.cartethyia.easyorange.ai.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.chat.AiChatAppService;
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
    @DisplayName("used + maxPerCall 越过日限即 true，未越即 false（流式预检与循环中途共用同一判据）")
    void exhausted_acrossDailyLimit() {
        when(budgetStore.getTodayUsage("chat"))
                .thenReturn(Optional.of(new TokenBudgetStorePort.TokenUsage(299_000, 0, 0)));
        assertThat(guard(PropertyBindings.bind(AiProperties.class)).exhausted()).isTrue();

        when(budgetStore.getTodayUsage("chat"))
                .thenReturn(Optional.of(new TokenBudgetStorePort.TokenUsage(1000, 0, 0)));
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
        when(budgetStore.getTodayUsage("chat"))
                .thenReturn(Optional.of(new TokenBudgetStorePort.TokenUsage(20_000, 0, 0)));

        assertThat(guard(properties).exhausted()).isTrue();
    }

    @Test
    @DisplayName("场景配置缺失 -> 走与 @TokenBudget 注解同值的兜底（不静默当作不限额）")
    void exhausted_fallsBackWhenScenarioMissing() {
        when(budgetStore.getTodayUsage("chat"))
                .thenReturn(Optional.of(new TokenBudgetStorePort.TokenUsage(299_000, 0, 0)));

        assertThat(guard(PropertyBindings.bind(AiProperties.class)).exhausted()).isTrue();
    }

    @Test
    @DisplayName("日限配 0 = 不限，任何用量都不判超限")
    void exhausted_zeroDailyLimitNeverExhausts() {
        var properties = PropertyBindings.bind(AiProperties.class, "budget.scenarios.chat.daily-token-limit", "0");
        when(budgetStore.getTodayUsage("chat"))
                .thenReturn(Optional.of(new TokenBudgetStorePort.TokenUsage(9_999_999, 0, 0)));

        assertThat(guard(properties).exhausted()).isFalse();
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
}
