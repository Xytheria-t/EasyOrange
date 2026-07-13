package com.cartethyia.easyorange.ai.budget;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBudgetAspect 测试")
class TokenBudgetAspectTest {

    @Mock
    private ProceedingJoinPoint pjp;

    private InMemoryTokenBudgetStore store;
    private TokenBudgetAspect aspect;

    @BeforeEach
    void setUp() {
        store = new InMemoryTokenBudgetStore();
        aspect = new TokenBudgetAspect(store);
    }

    @Test
    @DisplayName("预算未超限时正常调用并返回结果")
    void aroundBudget_withinLimit_proceedsAndReturnsResult() throws Throwable {
        var annotation = mockBudget("pricing", 100, 1000);
        when(pjp.proceed()).thenReturn("AI response");

        var result = aspect.aroundBudget(pjp, annotation);

        assertThat(result).isEqualTo("AI response");
        verify(pjp).proceed();
    }

    @Test
    @DisplayName("预算未超限时调用后记录用量")
    void aroundBudget_withinLimit_recordsUsage() throws Throwable {
        var annotation = mockBudget("pricing", 100, 1000);
        when(pjp.proceed()).thenReturn("AI response");

        aspect.aroundBudget(pjp, annotation);

        var usage = store.getTodayUsage("pricing");
        assertThat(usage).isPresent();
        assertThat(usage.get().total()).isEqualTo(100);
    }

    @Test
    @DisplayName("累计用量超 dailyTokenLimit 时抛 TokenBudgetExceededException")
    void aroundBudget_exceedsDailyLimit_throwsException() throws Throwable {
        var annotation = mockBudget("pricing", 600, 1000);
        store.recordUsage("pricing", 500, 0);

        assertThatThrownBy(() -> aspect.aroundBudget(pjp, annotation))
                .isInstanceOf(TokenBudgetExceededException.class)
                .hasMessageContaining("pricing")
                .hasMessageContaining("1000");

        verify(pjp, never()).proceed();
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

    /**
     * 创建模拟的 @TokenBudget 注解实例。
     */
    private TokenBudget mockBudget(String scenario, int maxPerCall, int dailyLimit) {
        var annotation = mock(TokenBudget.class);
        when(annotation.scenario()).thenReturn(scenario);
        when(annotation.maxTokensPerCall()).thenReturn(maxPerCall);
        when(annotation.dailyTokenLimit()).thenReturn(dailyLimit);
        return annotation;
    }
}
