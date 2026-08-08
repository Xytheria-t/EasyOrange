package com.cartethyia.easyorange.ai.config;

import com.cartethyia.easyorange.ai.budget.InMemoryTokenBudgetStore;
import com.cartethyia.easyorange.ai.budget.TokenBudgetStore;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
@RequiredArgsConstructor
public class AiConfig {

    @Bean
    @ConditionalOnMissingBean(TokenBudgetStore.class)
    public TokenBudgetStore tokenBudgetStore() {
        return new InMemoryTokenBudgetStore();
    }
}
