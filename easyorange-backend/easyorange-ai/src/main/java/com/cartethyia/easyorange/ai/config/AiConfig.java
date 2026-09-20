package com.cartethyia.easyorange.ai.config;

import com.cartethyia.easyorange.ai.adapter.outbound.budget.InMemoryTokenBudgetStore;
import com.cartethyia.easyorange.ai.adapter.outbound.budget.RedisTokenBudgetStore;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
@RequiredArgsConstructor
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
    public TokenBudgetStore redisTokenBudgetStore(ObjectProvider<StringRedisTemplate> redisProvider) {
        return new RedisTokenBudgetStore(redisProvider);
    }

    @Bean
    @ConditionalOnMissingBean(TokenBudgetStore.class)
    public TokenBudgetStore tokenBudgetStore() {
        return new InMemoryTokenBudgetStore();
    }
}
