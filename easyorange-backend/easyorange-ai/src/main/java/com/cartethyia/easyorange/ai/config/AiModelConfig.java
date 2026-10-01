package com.cartethyia.easyorange.ai.config;

import com.cartethyia.easyorange.ai.adapter.outbound.llm.UnconfiguredChatModel;
import com.cartethyia.easyorange.ai.adapter.outbound.llm.UnconfiguredEmbeddingModel;
import com.openai.client.OpenAIClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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

/**
 * Spring AI 模型装配 — 五个模型 bean 全手动创建（决策 / 文本 / 视觉 / 评审 / Embedding）：
 * 文本与视觉是两家 OpenAI 兼容供应商（部署实测用阿里云百炼，两者同端点同 key），base-url 与 api-key 可各自独立，
 * 单一 {@code spring.ai.openai.*} 自动配置表达不了，统一经 {@link OpenAiSetup#setupSyncClient} 手动
 * 构造 {@link OpenAIClient}；定义 bean 后自动配置的 {@code @ConditionalOnMissingBean} 自动退让，
 * 不会产生重复 bean。重试与并发隔离由 openai-java 客户端内置（{@code maxRetries} / 连接池）承担。
 * <p>
 * key 缺失时装配 {@link UnconfiguredChatModel} / {@link UnconfiguredEmbeddingModel} 占位 bean
 * （调用即抛「AI 模型未配置」，服务层现有 try/catch 降级）——应用无需任何 AI key 即可启动。
 */
@Configuration
public class AiModelConfig {

    private static final int MAX_RETRIES = 2;

    /**
     * 文本模型，业务服务默认注入的 {@code ChatModel}。
     * options 必须同时带 baseUrl / apiKey：Builder 需要同步与异步两个客户端，异步的由 options 里的
     * 连接参数自行装配，缺凭据会抛 IllegalStateException 导致 key 非空时上下文启动失败
     * （key 为空走占位 bean 分支，恰好掩盖该问题）。
     */
    @Bean
    @Primary
    public ChatModel chatModel(AiProperties props, ObservationRegistry obs, MeterRegistry meters) {
        var text = props.text();
        if (hasNoText(text.apiKey())) {
            return new UnconfiguredChatModel("easyorange.ai.text.api-key 为空，请配置 AI_TEXT_API_KEY");
        }
        return OpenAiChatModel.builder()
                .openAiClient(syncClient(text.baseUrl(), text.apiKey(), text.model(), text.timeout(), obs, meters))
                .options(OpenAiChatOptions.builder()
                        .baseUrl(text.baseUrl())
                        .apiKey(text.apiKey())
                        .model(text.model())
                        // 流式默认不带用量分片，打开后末帧回报 token 用量 —— 流式对话的预算记账依赖它
                        .streamUsage(true)
                        .build())
                .observationRegistry(obs)
                .build();
    }

    /**
     * 工具决策模型 — 工具调用循环每轮「选哪个工具」，由 {@code chat_tool} 场景路由到这里。
     * 与生成模型分开配的理由：决策的产物只是几十字符的工具参数 JSON，纯路由任务，而推理模型在这种
     * 短决策上仍会产出长度不定的思考内容（实测同一决策在几十到上千字符间波动，直接决定单轮延迟，
     * 同一问题 3.8s ~ 13.5s）；循环每轮串行一次决策，N 轮就是 N 倍开销 —— 这里配快模型，强模型留给
     * 最终生成。{@code router-model} 留空时与生成同模型，不改变既有行为。
     * 非 {@code @Primary}：业务服务默认注入的仍是 {@link #chatModel}。
     */
    @Bean
    public ChatModel decisionChatModel(AiProperties props, ObservationRegistry obs, MeterRegistry meters) {
        var text = props.text();
        if (hasNoText(text.apiKey())) {
            return new UnconfiguredChatModel("easyorange.ai.text.api-key 为空，请配置 AI_TEXT_API_KEY");
        }
        String model = hasNoText(text.routerModel())
                ? text.model()
                : text.routerModel().trim();
        return OpenAiChatModel.builder()
                .openAiClient(syncClient(text.baseUrl(), text.apiKey(), model, text.timeout(), obs, meters))
                .options(OpenAiChatOptions.builder()
                        .baseUrl(text.baseUrl())
                        .apiKey(text.apiKey())
                        .model(model)
                        .build())
                .observationRegistry(obs)
                .build();
    }

    /** 视觉模型，拍照上架图片识别专用；options 带 baseUrl / apiKey 的理由同 {@link #chatModel}。 */
    @Bean
    public ChatModel visionChatModel(AiProperties props, ObservationRegistry obs, MeterRegistry meters) {
        var vision = props.vision();
        if (hasNoText(vision.apiKey())) {
            return new UnconfiguredChatModel("easyorange.ai.vision.api-key 为空，请配置 AI_VISION_API_KEY");
        }
        return OpenAiChatModel.builder()
                .openAiClient(
                        syncClient(vision.baseUrl(), vision.apiKey(), vision.model(), vision.timeout(), obs, meters))
                .options(OpenAiChatOptions.builder()
                        .baseUrl(vision.baseUrl())
                        .apiKey(vision.apiKey())
                        .model(vision.model())
                        .build())
                .observationRegistry(obs)
                .build();
    }

    /**
     * 评审模型（LLM-as-Judge）— 金标准集回归的打分员，由 {@code judge} 场景路由到这里。
     * 与生成模型分家族的理由：同一模型给自己风格的输出打分系统性偏高，Judge 分数要对外可引用
     * 就必须换评审员；默认落百炼 qwen（与视觉槽同账号同 key），换评审员只改 yaml。
     */
    @Bean
    public ChatModel judgeChatModel(AiProperties props, ObservationRegistry obs, MeterRegistry meters) {
        var judge = props.judge();
        if (hasNoText(judge.apiKey())) {
            return new UnconfiguredChatModel("easyorange.ai.judge.api-key 为空，请配置 AI_JUDGE_API_KEY 或 AI_VISION_API_KEY");
        }
        return OpenAiChatModel.builder()
                .openAiClient(syncClient(judge.baseUrl(), judge.apiKey(), judge.model(), judge.timeout(), obs, meters))
                .options(OpenAiChatOptions.builder()
                        .baseUrl(judge.baseUrl())
                        .apiKey(judge.apiKey())
                        .model(judge.model())
                        .build())
                .observationRegistry(obs)
                .build();
    }

    /** Embedding 模型 — DashScope text-embedding-v3（OpenAI 兼容端点），维度对齐约束见 {@link AiProperties#embedding()}。 */
    @Bean
    public EmbeddingModel embeddingModel(AiProperties props, ObservationRegistry obs, MeterRegistry meters) {
        var embedding = props.embedding();
        if (hasNoText(embedding.apiKey())) {
            return new UnconfiguredEmbeddingModel("easyorange.ai.embedding.api-key 为空，请配置 EMBEDDING_API_KEY");
        }
        return OpenAiEmbeddingModel.builder()
                .openAiClient(syncClient(
                        embedding.baseUrl(), embedding.apiKey(), embedding.model(), embedding.timeout(), obs, meters))
                .options(OpenAiEmbeddingOptions.builder()
                        .model(embedding.model())
                        .dimensions(embedding.dimensions())
                        .build())
                .observationRegistry(obs)
                .build();
    }

    private static boolean hasNoText(String value) {
        return value == null || value.isBlank();
    }

    /** OpenAI 兼容供应商的统一同步客户端装配 — 非本项目使用的 Azure/stubbing/自定义头等能力传空值（均走 apiKey + base-url 直连）。 */
    private static OpenAIClient syncClient(
            String baseUrl,
            String apiKey,
            String model,
            int timeoutMillis,
            ObservationRegistry observationRegistry,
            MeterRegistry meterRegistry) {
        return OpenAiSetup.setupSyncClient(
                baseUrl,
                apiKey,
                null, // credential — 仅 apiKey 认证
                null, // azureServiceVersion
                null, // organizationId
                null, // projectId
                false, // streaming
                false, // parallelToolCalls
                model,
                Duration.ofMillis(timeoutMillis),
                MAX_RETRIES,
                null, // proxy
                Map.of(), // defaultHeaders
                observationRegistry,
                meterRegistry,
                List.of()); // OpenAiHttpClientBuilderCustomizer
    }
}
