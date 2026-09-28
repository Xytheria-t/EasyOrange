package com.cartethyia.easyorange.ai.config;

import com.cartethyia.easyorange.ai.adapter.outbound.budget.InMemoryTokenBudgetStore;
import com.cartethyia.easyorange.ai.adapter.outbound.budget.RedisTokenBudgetStore;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * AI 预算存储的装配 — {@code easyorange.ai.budget.store} 选实现，缺省落内存版。
 * <p>
 * {@link AiProperties} 不在这里 {@code @EnableConfigurationProperties}：业务模块的属性绑定统一由
 * 启动类的 {@code @ConfigurationPropertiesScan} 负责，两处都挂会重复注册。
 */
@Configuration
public class AiConfig {

    /**
     * Redis 版预算存储（{@code easyorange.ai.budget.store=redis}）——多实例部署共享日预算。
     * <p>
     * 必须声明在内存版之前：同配置类内 {@code @Bean} 按声明顺序注册，{@code @ConditionalOnMissingBean}
     * 才看得到它已存在（顺序反了会两个都装配）。Redis 未装配时用 {@code ObjectProvider} 惰性取，
     * 不影响启动。
     */
    @Bean
    @ConditionalOnProperty(name = "easyorange.ai.budget.store", havingValue = "redis")
    public TokenBudgetStorePort redisTokenBudgetStore(
            ObjectProvider<StringRedisTemplate> redisProvider, MeterRegistry meterRegistry) {
        return new RedisTokenBudgetStore(redisProvider, meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(TokenBudgetStorePort.class)
    public TokenBudgetStorePort tokenBudgetStore() {
        return new InMemoryTokenBudgetStore();
    }
}
