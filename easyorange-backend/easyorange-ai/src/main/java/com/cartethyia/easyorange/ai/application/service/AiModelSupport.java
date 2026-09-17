package com.cartethyia.easyorange.ai.application.service;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.port.AiCallLogPort;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStore;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

/**
 * Spring AI 调用小工具 — 收敛 system+user 双消息、JSON 结构化输出、Embedding、
 * 多图视觉识别这几类重复调用模式，避免每个服务重复组装 {@link Prompt}。
 * <p>
 * 带 {@link AiCallScope} 的重载在调用前后做两件横切记账（两者都是「调用副产物」，失败绝不影响业务）：
 * <ul>
 *   <li>{@link AiCallLogPort} — 记一条 eo_ai_call_log（LLM-as-Judge 离线评估数据源）；</li>
 *   <li>{@link TokenBudgetStore} — 记本次调用的真实 token 用量（场景键 = scope 小写），
 *       供 {@code TokenBudgetAspect} / 流式链路的预算前置检查累计。供应商未回报用量时
 *       （embedding 接口不返回 usage、部分兼容端点忽略 stream_options）退化为按场景上限估算，
 *       宁可高估也不让预算静默失效。</li>
 * </ul>
 * <p>
 * 需要按角色分隔多轮消息（system / 历史 user+assistant / 当前 user）的调用走
 * {@link #callText(ChatModel, AiCallScope, List)} 重载 —— 历史不进单条 user 消息，
 * 前缀稳定才能命中供应商的上下文缓存（KV cache 折扣计价）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelSupport {

    private final AiCallLogPort callLogRecorder;
    private final TokenBudgetStore budgetStore;
    private final AiProperties aiProperties;

    /**
     * 普通文本生成：system + user 双消息。
     */
    public String callText(ChatModel chatModel, String systemPrompt, String userMessage) {
        return outputText(
                chatModel.call(new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)))));
    }

    /**
     * 普通文本生成（带调用日志与预算记账）：system + user 双消息，成功后记录 scope/model/耗时/用量。
     */
    public String callText(ChatModel chatModel, AiCallScope scope, String systemPrompt, String userMessage) {
        return recordCall(
                scope,
                chatModel,
                systemPrompt + userMessage,
                () -> chatOutcome(chatModel.call(
                        new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage))))));
    }

    /**
     * 普通文本生成（多角色消息，带调用日志与预算记账）— 多轮对话专用。
     * <p>
     * 调用方按 [system, 历史 user/assistant …, 当前 user] 组装；历史不再拼进当前 user 消息，
     * 使跨轮次前缀保持稳定（供应商上下文缓存按前缀命中计价）。
     */
    public String callText(ChatModel chatModel, AiCallScope scope, List<Message> messages) {
        return recordCall(
                scope, chatModel, joinTexts(messages), () -> chatOutcome(chatModel.call(new Prompt(messages))));
    }

    /**
     * JSON 结构化输出：在 system + user 双消息之上追加 {@code response_format=json_object}，
     * 提示模型返回合法 JSON（解析与降级仍由调用方 ObjectMapper + try/catch 承担）。
     * <p>
     * per-request options 必须继承模型的连接与模型名：只设 {@code responseFormat} 时 {@code model} 为 null，
     * openai-java 客户端会回退到 SDK 默认模型名（{@code gpt-5-mini}），对非 OpenAI 供应商直接 404 ——
     * 走本方法的所有 AI 决策点与 LLM-as-Judge 会整体静默降级。
     */
    public String callJson(ChatModel chatModel, String systemPrompt, String userMessage) {
        var jsonOptions = OpenAiChatOptions.builder()
                .responseFormat(OpenAiChatModel.ResponseFormat.builder()
                        .type(OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT)
                        .build());
        inheritConnection(jsonOptions, chatModel);
        return outputText(chatModel.call(new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)), jsonOptions.build())));
    }

    /**
     * JSON 结构化输出（带调用日志与预算记账）：同 {@link #callJson}，记录 scope/model/耗时/用量。
     */
    public String callJson(ChatModel chatModel, AiCallScope scope, String systemPrompt, String userMessage) {
        return recordCall(
                scope,
                chatModel,
                systemPrompt + userMessage,
                () -> chatOutcome(chatModel.call(new Prompt(
                        List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)),
                        jsonOptions(chatModel)))));
    }

    /**
     * 流式文本生成（带调用日志与预算记账）：逐 token 回调 {@code tokenConsumer}，阻塞至流结束返回完整文本。
     * <p>
     * 供 SSE 场景使用（AiChatService 把 token 回调接到 SseEmitter）；调用日志/耗时/用量统计
     * 与 {@link #callText} 一致，落库的 response_text 是完整拼接结果（Judge 数据源不缺流式调用）。
     */
    public String callTextStream(
            ChatModel chatModel,
            AiCallScope scope,
            String systemPrompt,
            String userMessage,
            Consumer<String> tokenConsumer) {
        return callTextStream(
                chatModel,
                scope,
                List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)),
                tokenConsumer);
    }

    /**
     * 流式文本生成（多角色消息）：同 {@link #callText(ChatModel, AiCallScope, List)}，
     * 逐 token 回调并把整段回答返回给调用方。
     */
    public String callTextStream(
            ChatModel chatModel, AiCallScope scope, List<Message> messages, Consumer<String> tokenConsumer) {
        return recordCall(scope, chatModel, joinTexts(messages), () -> {
            var sb = new StringBuilder();
            var usage = new UsageAccumulator();
            chatModel.stream(new Prompt(messages))
                    .doOnNext(response -> {
                        usage.accept(response);
                        String token = outputText(response);
                        if (token != null && !token.isEmpty()) {
                            sb.append(token);
                            tokenConsumer.accept(token);
                        }
                    })
                    .blockLast();
            return new CallOutcome<>(sb.toString(), usage.result());
        });
    }

    /**
     * 文本向量化：{@code float[]} 转 {@code List<Float>}（ES kNN 查询需要的形态）。
     */
    public List<Float> embed(EmbeddingModel embeddingModel, String text) {
        float[] arr = embeddingModel.embed(text);
        var list = new ArrayList<Float>(arr.length);
        for (float value : arr) {
            list.add(value);
        }
        return list;
    }

    /**
     * 文本向量化（带调用日志与预算记账）：同 {@link #embed}，响应不落库只记成功与否；
     * embedding 接口不回报 usage，用量按场景上限估算（见类注释）。
     */
    public List<Float> embed(EmbeddingModel embeddingModel, AiCallScope scope, String text) {
        return recordCall(
                scope, embeddingModel, "embed" + text, () -> new CallOutcome<>(embed(embeddingModel, text), null));
    }

    /**
     * 多图视觉识别：图片以 {@link Media}（URL）随提示词一并交给视觉模型。
     */
    public String analyzeImages(ChatModel visionChatModel, AiCallScope scope, List<String> imageUrls, String prompt) {
        List<Media> media = imageUrls.stream()
                .map(url -> Media.builder()
                        .mimeType(Media.Format.IMAGE_JPEG)
                        .data(URI.create(url))
                        .build())
                .toList();
        Message userMessage = UserMessage.builder().text(prompt).media(media).build();
        return recordCall(
                scope, visionChatModel, prompt, () -> chatOutcome(visionChatModel.call(new Prompt(userMessage))));
    }

    /**
     * 提取模型文本输出；模型可能不返回结果（返回空串），避免 NPE。
     */
    private static String outputText(ChatResponse response) {
        var result = response.getResult();
        return result != null ? result.getOutput().getText() : "";
    }

    /**
     * 成色等级（"1"~"4"）→ 中文标签。多个 AI 服务拼 prompt 时共用，避免各自复制一份映射。
     */
    public static String formatCondition(String conditionLevel) {
        if (conditionLevel == null) {
            return "未知";
        }
        return switch (conditionLevel) {
            case "1" -> "全新";
            case "2" -> "九五新";
            case "3" -> "八五新";
            case "4" -> "七成新";
            default -> "未知";
        };
    }

    private static OpenAiChatOptions jsonOptions(ChatModel chatModel) {
        var jsonOptions = OpenAiChatOptions.builder()
                .responseFormat(OpenAiChatModel.ResponseFormat.builder()
                        .type(OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT)
                        .build());
        inheritConnection(jsonOptions, chatModel);
        return jsonOptions.build();
    }

    /**
     * per-request options 继承模型的连接与模型名（缺 model 时 openai-java 会回退 SDK 默认模型，
     * 对非 OpenAI 供应商直接 404 —— 见 {@link #callJson}）。
     */
    private static void inheritConnection(OpenAiChatOptions.Builder builder, ChatModel chatModel) {
        if (chatModel instanceof OpenAiChatModel openAiModel
                && openAiModel.getDefaultOptions() instanceof OpenAiChatOptions defaults) {
            builder.baseUrl(defaults.getBaseUrl()).apiKey(defaults.getApiKey()).model(defaults.getModel());
        }
    }

    private static String joinTexts(List<Message> messages) {
        return messages.stream().map(Message::getText).collect(Collectors.joining("\n"));
    }

    /** 把 ChatResponse 包成「文本 + 用量」，未回报用量时 usage 为 null。 */
    private static CallOutcome<String> chatOutcome(@Nullable ChatResponse response) {
        if (response == null) {
            return new CallOutcome<>("", null);
        }
        Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;
        Integer in = usage != null ? usage.getPromptTokens() : null;
        Integer out = usage != null ? usage.getCompletionTokens() : null;
        boolean reported = (in != null && in > 0) || (out != null && out > 0);
        return new CallOutcome<>(outputText(response), reported ? usage : null);
    }

    private <T> T recordCall(AiCallScope scope, Object model, String promptText, Supplier<CallOutcome<T>> supplier) {
        long start = System.nanoTime();
        CallOutcome<T> outcome = null;
        boolean success = false;
        String errorMsg = null;
        try {
            outcome = supplier.get();
            success = true;
            return outcome.value();
        } catch (Exception e) {
            errorMsg = e.getMessage();
            throw e;
        } finally {
            recordCallLog(scope, model, promptText, outcome, start, success, errorMsg);
            recordBudgetUsage(scope, outcome);
        }
    }

    private void recordCallLog(
            AiCallScope scope,
            Object model,
            String promptText,
            @Nullable CallOutcome<?> outcome,
            long startNanos,
            boolean success,
            @Nullable String errorMsg) {
        try {
            String response = outcome != null && outcome.value() instanceof String s ? s : null;
            callLogRecorder.record(
                    scope.name(),
                    model.getClass().getSimpleName(),
                    md5(promptText),
                    response,
                    (System.nanoTime() - startNanos) / 1_000_000,
                    success,
                    errorMsg);
        } catch (Exception e) {
            // recorder 内部已吞异常，此处兜底
        }
    }

    /**
     * 记录本次调用的 token 用量（场景键与 {@code @TokenBudget(scenario=...)} 对齐）。
     * <p>
     * 有真实用量就用真实值；供应商未回报用量（embedding / 流式未带 usage）时退化为场景配置的单次上限，
     * 保证日预算仍能累计 —— 估算值偏高，但比「预算永远为 0、限流静默失效」安全。
     */
    private void recordBudgetUsage(AiCallScope scope, @Nullable CallOutcome<?> outcome) {
        if (!aiProperties.budget().enabled()) {
            return;
        }
        try {
            String scenario = scope.budgetScenario();
            Usage usage = outcome != null ? outcome.usage() : null;
            int inputTokens = usage != null && usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
            int outputTokens = usage != null && usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
            if (inputTokens + outputTokens > 0) {
                budgetStore.recordUsage(scenario, inputTokens, outputTokens);
                return;
            }
            var configured = aiProperties.budget().resolve(scenario);
            if (configured != null) {
                budgetStore.recordUsage(scenario, configured.maxTokensPerCall(), 0);
            }
        } catch (Exception e) {
            log.debug("Token 用量记账失败（不影响调用）: {}", e.getMessage());
        }
    }

    private static String md5(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }

    /** 调用结果 + 供应商回报的用量（未回报时为 null）。 */
    private record CallOutcome<T>(T value, @Nullable Usage usage) {}

    /** 流式响应里用量只出现在末尾分片，且可能整段缺失 —— 取最后一个非空用量。 */
    private static final class UsageAccumulator implements Consumer<ChatResponse> {

        private Usage latest;

        @Override
        public void accept(ChatResponse response) {
            if (response == null || response.getMetadata() == null) {
                return;
            }
            Usage usage = response.getMetadata().getUsage();
            if (usage == null) {
                return;
            }
            Integer in = usage.getPromptTokens();
            Integer out = usage.getCompletionTokens();
            if ((in != null && in > 0) || (out != null && out > 0)) {
                latest = usage;
            }
        }

        @Nullable
        Usage result() {
            return latest;
        }
    }
}
