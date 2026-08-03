package com.cartethyia.easyorange.ai.config;

import com.openai.client.OpenAIClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.setup.OpenAiSetup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Spring AI 模型装配 — 三个模型 bean 全手动创建。
 * <p>
 * 项目同时使用两个 OpenAI 兼容供应商（DeepSeek 文本 / DashScope 视觉 + Embedding），
 * base-url 与 api-key 均不同，无法用单一 {@code spring.ai.openai.*} 自动配置表达，
 * 因此统一经由 {@link OpenAiSetup#setupSyncClient} 手动构造 {@link OpenAIClient}。
 * 定义 {@code OpenAiChatModel}/{@code OpenAiEmbeddingModel} bean 后，
 * OpenAI 自动配置的 {@code @ConditionalOnMissingBean} 会自动退让，不会产生重复 bean。
 * <p>
 * 重试由 openai-java 客户端内置（{@code maxRetries}）承担，替代原 Resilience4j Retry；
 * 并发隔离由 openai-java 客户端内置连接池承担，替代原 Bulkhead。
 */
@Configuration
public class AiModelConfig {

    private static final int MAX_RETRIES = 2;

    /**
     * 文本模型 — DeepSeek，业务服务默认注入的 {@code ChatModel}。
     */
    @Bean
    @Primary
    public OpenAiChatModel chatModel(AiProperties props, ObservationRegistry observationRegistry,
                                     MeterRegistry meterRegistry) {
        var deepseek = props.getDeepseek();
        return OpenAiChatModel.builder()
                .openAiClient(setupClient(deepseek.getBaseUrl(), deepseek.getApiKey(), deepseek.getModel(),
                        deepseek.getTimeout(), observationRegistry, meterRegistry))
                .options(OpenAiChatOptions.builder().model(deepseek.getModel()).build())
                .observationRegistry(observationRegistry)
                .build();
    }

    /**
     * 视觉模型 — Qwen-VL（DashScope OpenAI 兼容端点），拍照上架图片识别专用。
     */
    @Bean
    public OpenAiChatModel visionChatModel(AiProperties props, ObservationRegistry observationRegistry,
                                           MeterRegistry meterRegistry) {
        var qwenVl = props.getQwenVl();
        return OpenAiChatModel.builder()
                .openAiClient(setupClient(qwenVl.getBaseUrl(), qwenVl.getApiKey(), qwenVl.getModel(),
                        qwenVl.getTimeout(), observationRegistry, meterRegistry))
                .options(OpenAiChatOptions.builder().model(qwenVl.getModel()).build())
                .observationRegistry(observationRegistry)
                .build();
    }

    /**
     * Embedding 模型 — DashScope text-embedding-v3（OpenAI 兼容端点）。
     */
    @Bean
    public OpenAiEmbeddingModel embeddingModel(AiProperties props, ObservationRegistry observationRegistry,
                                               MeterRegistry meterRegistry) {
        var embedding = props.getEmbedding();
        return OpenAiEmbeddingModel.builder()
                .openAiClient(setupClient(embedding.getBaseUrl(), embedding.getApiKey(), embedding.getModel(),
                        embedding.getTimeout(), observationRegistry, meterRegistry))
                .options(OpenAiEmbeddingOptions.builder()
                        .model(embedding.getModel())
                        .dimensions(embedding.getDimensions())
                        .build())
                .observationRegistry(observationRegistry)
                .build();
    }

    private static OpenAIClient setupClient(String baseUrl, String apiKey, String model, int timeoutMillis,
                                            ObservationRegistry observationRegistry, MeterRegistry meterRegistry) {
        return OpenAiSetup.setupSyncClient(
                baseUrl, apiKey, null, null, null, null, false, false,
                model, Duration.ofMillis(timeoutMillis), MAX_RETRIES, null, Map.of(),
                observationRegistry, meterRegistry, List.of());
    }
}
