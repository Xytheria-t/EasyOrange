package com.cartethyia.easyorange.ai.config;

import com.openai.client.OpenAIClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
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
 * <p>
 * <b>可选功能降级装配</b>：key 缺失时装配 {@link UnconfiguredChatModel}/{@link UnconfiguredEmbeddingModel}
 * 占位 bean（调用即抛「AI 模型未配置」异常），由服务层现有 try/catch 降级为 null——
 * 应用无需任何 AI key 即可启动，与 AGENTS.md「AI 密钥可选、不影响应用启动」的契约一致。
 */
@Configuration
public class AiModelConfig {

    private static final int MAX_RETRIES = 2;

    /**
     * 文本模型 — DeepSeek，业务服务默认注入的 {@code ChatModel}。
     */
    @Bean
    @Primary
    public ChatModel chatModel(AiProperties props, ObservationRegistry observationRegistry,
                               MeterRegistry meterRegistry) {
        var deepseek = props.getDeepseek();
        if (hasNoText(deepseek.getApiKey())) {
            return new UnconfiguredChatModel("easyorange.ai.deepseek.api-key 为空，请配置 DEEPSEEK_API_KEY");
        }
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
    public ChatModel visionChatModel(AiProperties props, ObservationRegistry observationRegistry,
                                     MeterRegistry meterRegistry) {
        var qwenVl = props.getQwenVl();
        if (hasNoText(qwenVl.getApiKey())) {
            return new UnconfiguredChatModel("easyorange.ai.qwen-vl.api-key 为空，请配置 QWEN_VL_API_KEY");
        }
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
    public EmbeddingModel embeddingModel(AiProperties props, ObservationRegistry observationRegistry,
                                         MeterRegistry meterRegistry) {
        var embedding = props.getEmbedding();
        if (hasNoText(embedding.getApiKey())) {
            return new UnconfiguredEmbeddingModel("easyorange.ai.embedding.api-key 为空，请配置 EMBEDDING_API_KEY");
        }
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

    private static boolean hasNoText(String value) {
        return value == null || value.isBlank();
    }

    private static OpenAIClient setupClient(String baseUrl, String apiKey, String model, int timeoutMillis,
                                            ObservationRegistry observationRegistry, MeterRegistry meterRegistry) {
        return OpenAiSetup.setupSyncClient(
                baseUrl, apiKey, null, null, null, null, false, false,
                model, Duration.ofMillis(timeoutMillis), MAX_RETRIES, null, Map.of(),
                observationRegistry, meterRegistry, List.of());
    }
}
