package com.cartethyia.easyorange.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.adapter.outbound.budget.InMemoryTokenBudgetStore;
import com.cartethyia.easyorange.ai.adapter.outbound.budget.RedisTokenBudgetStore;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * 预算存储装配 — 默认内存版；{@code easyorange.ai.budget.store=redis} 时切 Redis 版。
 * <p>
 * 这里把「Redis Bean 必须声明在内存版之前」这条顺序要求断言化：顺序反了
 * {@code @ConditionalOnMissingBean} 会两个都注册，容器里出现两个 {@link TokenBudgetStore}（注入点报
 * NoUniqueBeanDefinitionException），这个测试先红。
 */
@DisplayName("TokenBudgetStore 装配（memory / redis 切换）")
class TokenBudgetStoreWiringTest {

    @Test
    @DisplayName("未配置 store -> 内存版（默认，单实例 / 开发）")
    void defaultsToInMemoryStore() {
        try (var context = new AnnotationConfigApplicationContext(AiConfig.class)) {
            assertThat(context.getBean(TokenBudgetStore.class)).isInstanceOf(InMemoryTokenBudgetStore.class);
        }
    }

    @Test
    @DisplayName("store=redis -> Redis 版，且容器里只有这一个 TokenBudgetStore")
    void redisPropertySelectsRedisStore() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource("test", Map.of("easyorange.ai.budget.store", "redis")));
            // fail-open 计数依赖 MeterRegistry（生产由 actuator 提供），这里按同形态补上
            context.registerBean(MeterRegistry.class, SimpleMeterRegistry::new);
            context.register(AiConfig.class);
            context.refresh();

            assertThat(context.getBeansOfType(TokenBudgetStore.class)).hasSize(1);
            assertThat(context.getBean(TokenBudgetStore.class)).isInstanceOf(RedisTokenBudgetStore.class);
        }
    }
}
