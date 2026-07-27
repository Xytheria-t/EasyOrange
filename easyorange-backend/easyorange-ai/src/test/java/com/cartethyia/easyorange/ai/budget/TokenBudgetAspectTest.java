package com.cartethyia.easyorange.ai.budget;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.metrics.AiMetricsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
    private AiProperties aiProperties;
    private SimpleMeterRegistry registry;
    private AiMetricsService metricsService;
    private TokenBudgetAspect aspect;

    @BeforeEach
    void setUp() {
        store = new InMemoryTokenBudgetStore();
        aiProperties = new AiProperties();
        registry = new SimpleMeterRegistry();
        metricsService = new AiMetricsService(registry);
        aspect = new TokenBudgetAspect(store, aiProperties, metricsService);
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
    @DisplayName("预算未超限时调用后记录用量并上报 metrics")
    void aroundBudget_withinLimit_recordsUsageAndMetrics() throws Throwable {
        var annotation = mockBudget("pricing", 100, 1000);
        when(pjp.proceed()).thenReturn("AI response");

        aspect.aroundBudget(pjp, annotation);

        // 存储记录
        var usage = store.getTodayUsage("pricing");
        assertThat(usage).isPresent();
        assertThat(usage.get().total()).isEqualTo(100);

        // metrics 上报：usage counter + daily_total gauge
        var usageCounter = registry.find("easyorange.ai.token_budget.usage")
                .tag("scenario", "pricing").tag("outcome", "consumed").counter();
        assertThat(usageCounter).isNotNull();
        assertThat(usageCounter.count()).isEqualTo(100.0);

        var dailyGauge = registry.find("easyorange.ai.token_budget.daily_total")
                .tag("scenario", "pricing").gauge();
        assertThat(dailyGauge).isNotNull();
        assertThat(dailyGauge.value()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("累计用量超 dailyTokenLimit 时抛异常并上报 exceeded 指标")
    void aroundBudget_exceedsDailyLimit_throwsExceptionAndReportsMetric() throws Throwable {
        var annotation = mockBudget("pricing", 600, 1000);
        store.recordUsage("pricing", 500, 0);

        assertThatThrownBy(() -> aspect.aroundBudget(pjp, annotation))
                .isInstanceOf(TokenBudgetExceededException.class)
                .hasMessageContaining("pricing")
                .hasMessageContaining("1000");

        verify(pjp, never()).proceed();

        // exceeded counter 递增
        var exceededCounter = registry.find("easyorange.ai.token_budget")
                .tag("scenario", "pricing").tag("outcome", "exceeded").counter();
        assertThat(exceededCounter).isNotNull();
        assertThat(exceededCounter.count()).isEqualTo(1.0);
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
        var scenarioBudget = new AiProperties.Budget.ScenarioBudget();
        scenarioBudget.setMaxTokensPerCall(50);
        scenarioBudget.setDailyTokenLimit(200);
        aiProperties.getBudget().getScenarios().put("pricing", scenarioBudget);

        // 注解声明 1000/10000（应被配置覆盖，仅 scenario() 会被读取）
        var annotation = mock(TokenBudget.class);
        when(annotation.scenario()).thenReturn("pricing");

        // 累计 180 + 配置上限 50 = 230 > 配置日预算 200 → 超限
        store.recordUsage("pricing", 180, 0);

        assertThatThrownBy(() -> aspect.aroundBudget(pjp, annotation))
                .isInstanceOf(TokenBudgetExceededException.class)
                .hasMessageContaining("200"); // 配置的日预算，不是注解的 10000

        verify(pjp, never()).proceed();
    }

    @Test
    @DisplayName("配置未覆盖场景时回退到注解默认值")
    void aroundBudget_configMissing_fallsBackToAnnotation() throws Throwable {
        // aiProperties.budget.scenarios 为空（默认），应使用注解值
        var annotation = mockBudget("qa", 800, 5000);

        // 累计 4500 + 800 = 5300 > 5000 → 超限（用注解值判定）
        store.recordUsage("qa", 4500, 0);

        assertThatThrownBy(() -> aspect.aroundBudget(pjp, annotation))
                .isInstanceOf(TokenBudgetExceededException.class)
                .hasMessageContaining("5000");

        verify(pjp, never()).proceed();
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
