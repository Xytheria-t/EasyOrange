package com.cartethyia.easyorange.ai.adapter.outbound.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.application.support.TokenBudgetGuard;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.enums.AiResultCode;
import com.cartethyia.easyorange.ai.domain.exception.TokenBudgetExceededException;
import com.cartethyia.easyorange.ai.testsupport.PropertyBindings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBudgetAspect 测试")
class TokenBudgetAspectTest {

    @Mock
    private ProceedingJoinPoint pjp;

    private InMemoryTokenBudgetStore store;
    private AiProperties aiProperties;
    private TokenBudgetAspect aspect;

    @BeforeEach
    void setUp() {
        store = new InMemoryTokenBudgetStore();
        aiProperties = PropertyBindings.bind(AiProperties.class);
        aspect = new TokenBudgetAspect(new TokenBudgetGuard(store, aiProperties));
    }

    @Test
    @DisplayName("预算未超限时正常调用并返回结果（注解默认值兜底）")
    void aroundBudget_withinLimit_proceedsAndReturnsResult() throws Throwable {
        var annotation = mockBudget("pricing", 100, 1000);
        when(pjp.proceed()).thenReturn("AI response");

        var result = aspect.aroundBudget(pjp, annotation);

        assertThat(result).isEqualTo("AI response");
        verify(pjp).proceed();
    }

    @Test
    @DisplayName("预留完成即释放 — 切面不落真实用量（真实记账由 ModelSupport 从 ChatResponse 另行累加）")
    void aroundBudget_doesNotRecordUsage() throws Throwable {
        var annotation = mockBudget("pricing", 100, 1000);
        when(pjp.proceed()).thenReturn("AI response");

        aspect.aroundBudget(pjp, annotation);

        // 切面只做原子预留 + 释放：按 maxTokensPerCall 记账会与真实用量重复计数；预留会在今日条目
        // 留一个零值快照，判据是总量归零而非条目不存在
        assertThat(store.getTodayUsage("pricing").map(u -> u.total()).orElse(0)).isZero();
    }

    @Test
    @DisplayName("目标方法抛异常也释放预留 — finally 兜底，在途占用不滞留")
    void aroundBudget_proceedThrows_releasesReservation() throws Throwable {
        var annotation = mockBudget("pricing", 100, 1000);
        when(pjp.proceed()).thenThrow(new RuntimeException("model blew up"));

        assertThatThrownBy(() -> aspect.aroundBudget(pjp, annotation)).isInstanceOf(RuntimeException.class);

        assertThat(store.getTodayUsage("pricing").orElseThrow().reservedTokens())
                .isZero();
    }

    @Test
    @DisplayName("累计用量超 dailyTokenLimit 时抛异常且不执行目标方法")
    void aroundBudget_exceedsDailyLimit_throwsException() throws Throwable {
        var annotation = mockBudget("pricing", 600, 1000);
        store.recordUsage("pricing", 500, 0);

        assertThatThrownBy(() -> aspect.aroundBudget(pjp, annotation))
                .isInstanceOf(TokenBudgetExceededException.class)
                .hasMessageContaining("预算");

        verify(pjp, never()).proceed();
    }

    @Test
    @DisplayName("预算超限异常携带模块业务码 B8001，映射 400 而非落入 500 兜底")
    void aroundBudget_exceedsDailyLimit_carriesBusinessCode() throws Throwable {
        var annotation = mockBudget("pricing", 600, 1000);
        store.recordUsage("pricing", 500, 0);

        // 回归守卫：非 BaseBusinessException 子类的 RuntimeException 会被 GlobalExceptionHandler 判为
        // 编程错误 → 500，前端 isRetryable(>=500) 会把它当可重试错误自动重试
        assertThatThrownBy(() -> aspect.aroundBudget(pjp, annotation))
                .isInstanceOfSatisfying(TokenBudgetExceededException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(AiResultCode.TOKEN_BUDGET_EXCEEDED.getCode());
                    assertThat(ex.getCode()).isEqualTo("B8001");
                    assertThat(ex.getStatusCode().value()).isEqualTo(400);
                });
    }

    @Test
    @DisplayName("dailyTokenLimit=0 时永不超限")
    void aroundBudget_zeroDailyLimit_neverExceeds() throws Throwable {
        var annotation = mockBudget("pricing", 100, 0);
        when(pjp.proceed()).thenReturn("result");

        for (int i = 0; i < 10; i++) {
            var result = aspect.aroundBudget(pjp, annotation);
            assertThat(result).isEqualTo("result");
        }

        verify(pjp, times(10)).proceed();
    }

    @Test
    @DisplayName("恰好达到预算上限时不抛异常（等于不超）")
    void aroundBudget_exactlyAtLimit_doesNotThrow() throws Throwable {
        var annotation = mockBudget("pricing", 500, 1000);
        store.recordUsage("pricing", 500, 0);
        when(pjp.proceed()).thenReturn("result");

        var result = aspect.aroundBudget(pjp, annotation);

        assertThat(result).isEqualTo("result");
        verify(pjp).proceed();
    }

    @Test
    @DisplayName("不同场景的预算相互隔离")
    void aroundBudget_differentScenarios_isolated() throws Throwable {
        var pricingAnnotation = mockBudget("pricing", 600, 1000);
        var reviewAnnotation = mockBudget("review", 100, 1000);
        when(pjp.proceed()).thenReturn("result");

        store.recordUsage("pricing", 500, 0);
        assertThatThrownBy(() -> aspect.aroundBudget(pjp, pricingAnnotation))
                .isInstanceOf(TokenBudgetExceededException.class);

        var result = aspect.aroundBudget(pjp, reviewAnnotation);
        assertThat(result).isEqualTo("result");
    }

    @Test
    @DisplayName("配置覆盖注解默认值 — scenarios 中存在条目时用配置值")
    void aroundBudget_configOverridesAnnotation() throws Throwable {
        // 配置：pricing 场景日预算 200，单次上限 50
        aiProperties = PropertyBindings.bind(
                AiProperties.class,
                "budget.scenarios.pricing.max-tokens-per-call",
                "50",
                "budget.scenarios.pricing.daily-token-limit",
                "200");
        aspect = new TokenBudgetAspect(new TokenBudgetGuard(store, aiProperties));

        // 注解 mock 只提供 scenario()，限值应走配置覆盖
        var annotation = mock(TokenBudget.class);
        when(annotation.scenario()).thenReturn("pricing");

        store.recordUsage("pricing", 180, 0);

        // 配置值生效（注解限值未被读取）：180 + 50 > 200 → 超限
        assertThatThrownBy(() -> aspect.aroundBudget(pjp, annotation)).isInstanceOf(TokenBudgetExceededException.class);

        verify(pjp, never()).proceed();
    }

    @Test
    @DisplayName("配置未覆盖场景时回退到注解默认值")
    void aroundBudget_configMissing_fallsBackToAnnotation() throws Throwable {
        // budget.scenarios 为空（默认）→ 用注解值判定
        var annotation = mockBudget("qa", 800, 5000);

        // 4500 + 800 > 5000 → 超限（注解值生效）
        store.recordUsage("qa", 4500, 0);

        assertThatThrownBy(() -> aspect.aroundBudget(pjp, annotation)).isInstanceOf(TokenBudgetExceededException.class);

        verify(pjp, never()).proceed();
    }

    private TokenBudget mockBudget(String scenario, int maxPerCall, int dailyLimit) {
        var annotation = mock(TokenBudget.class);
        when(annotation.scenario()).thenReturn(scenario);
        when(annotation.maxTokensPerCall()).thenReturn(maxPerCall);
        when(annotation.dailyTokenLimit()).thenReturn(dailyLimit);
        return annotation;
    }
}
