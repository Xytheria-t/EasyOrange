package com.cartethyia.easyorange.ai.config;

import com.cartethyia.easyorange.ai.budget.InMemoryTokenBudgetStore;
import com.cartethyia.easyorange.ai.budget.TokenBudgetStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public RestClient deepseekRestClient(AiProperties aiProperties) {
        var props = aiProperties.getDeepseek();
        return createAiRestClient(props.getBaseUrl(), props.getApiKey(), props.getTimeout());
    }

    @Bean
    public RestClient qwenVlRestClient(AiProperties aiProperties) {
        var props = aiProperties.getQwenVl();
        return createAiRestClient(props.getBaseUrl(), props.getApiKey(), props.getTimeout());
    }

    @Bean
    @ConditionalOnMissingBean(TokenBudgetStore.class)
    public TokenBudgetStore tokenBudgetStore() {
        return new InMemoryTokenBudgetStore();
    }

    private static RestClient createAiRestClient(String baseUrl, String apiKey, int timeoutMillis) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMillis));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(requestFactory)
                .build();
    }
}