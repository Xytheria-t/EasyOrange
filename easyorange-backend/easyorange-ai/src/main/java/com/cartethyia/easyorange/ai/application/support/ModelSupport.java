package com.cartethyia.easyorange.ai.application.support;

import static com.cartethyia.easyorange.ai.application.support.ModelCallRecorder.UsageAccumulator;
import static com.cartethyia.easyorange.ai.application.support.ModelCallRecorder.usageOf;

import com.cartethyia.easyorange.ai.application.support.ModelCallRecorder.CallOutcome;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.enums.ModelCallScope;
import com.cartethyia.easyorange.ai.domain.exception.ChatStreamAbortedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Spring AI 调用小工具 — 收敛 system+user 双消息、JSON 结构化输出、原生 tool calling、Embedding、多模态结构化输出
 * 几类重复调用模式，避免每个服务重复组装 {@link Prompt}。
 * <p>
 * 带 {@link ModelCallScope} 的方法一律经 {@link ModelCallRecorder} 执行（落 eo_ai_call_log 与场景预算记账），治理出口唯一；
 * 唯一例外是 {@link #callJson}，专供 LLM-as-Judge 离线评估、刻意不记账。按角色分隔多轮消息走 {@link
 * #callText(ChatModel, ModelCallScope, List)} 重载 —— 历史不进单条 user 消息，前缀稳定才能命中供应商 KV cache 折扣计价。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelSupport {

    /** OpenAI 兼容协议 {@code tool_choice} 的枚举值：{@code required} = 必须从给定工具里选一个（口径见 {@link #toolOptions}）。 */
    private static final String TOOL_CHOICE_REQUIRED = "required";

    private final ModelCallRecorder callRecorder;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public String callText(ChatModel chatModel, ModelCallScope scope, String systemPrompt, String userMessage) {
        return callText(chatModel, scope, systemUser(systemPrompt, userMessage));
    }

    public String callText(ChatModel chatModel, ModelCallScope scope, List<Message> messages) {
        return callRecorder.record(
                scope,
                chatModel,
                joinTexts(messages),
                () -> chatOutcome(chatModel.call(new Prompt(messages, scopedOptions(chatModel, scope)))));
    }

    /** JSON 结构化输出（唯一不记账的调用）：追加 {@code response_format=json_object}，解析与降级由调用方承担。唯一调用方
     * {@code Judge} 只在金标准集回归跑批里跑，记账会让成本报表套娃、还要额外配评估场景预算，故刻意账外。 */
    public String callJson(ChatModel chatModel, String systemPrompt, String userMessage) {
        return outputText(
                chatModel.call(new Prompt(systemUser(systemPrompt, userMessage), jsonOptions(chatModel, null))));
    }

    /** 原生 tool calling（带记账）：空列表 = 模型没调工具，由调用方按决策失败处理；只发请求不执行工具。 */
    public List<AssistantMessage.ToolCall> callWithTools(
            ChatModel chatModel, ModelCallScope scope, List<Message> messages, List<ToolCallback> toolCallbacks) {
        return callRecorder.record(scope, chatModel, joinTexts(messages), () -> {
            ChatResponse response =
                    chatModel.call(new Prompt(messages, toolOptions(chatModel, toolCallbacks, maxTokensOf(scope))));
            return new CallOutcome<>(toolCallsOf(response), usageOf(response));
        });
    }

    /** 流式文本生成：逐 token 回调，阻塞至流结束返回完整文本；记账口径同 {@link #callText}（Judge 不走流式调用）。 */
    public String callTextStream(
            ChatModel chatModel,
            ModelCallScope scope,
            String systemPrompt,
            String userMessage,
            Consumer<String> tokenConsumer) {
        return callTextStream(chatModel, scope, systemUser(systemPrompt, userMessage), tokenConsumer);
    }

    public String callTextStream(
            ChatModel chatModel, ModelCallScope scope, List<Message> messages, Consumer<String> tokenConsumer) {
        return callRecorder.record(scope, chatModel, joinTexts(messages), () -> {
            var collected = new StringBuilder();
            var usage = new UsageAccumulator();
            try {
                chatModel.stream(new Prompt(messages, scopedOptions(chatModel, scope)))
                        .doOnNext(response -> {
                            usage.accept(response);
                            String token = outputText(response);
                            if (!token.isEmpty()) {
                                collected.append(token);
                                tokenConsumer.accept(token);
                            }
                        })
                        .blockLast();
            } catch (ChatStreamAbortedException e) {
                // 客户端离开时把已生成的半截回答带出去：上层据此把中断轮落会话历史，刷新后追问才有上下文
                throw collected.isEmpty() ? e : new ChatStreamAbortedException(collected.toString(), e);
            }
            return new CallOutcome<>(collected.toString(), usage.result());
        });
    }

    /** 文本向量化（ES kNN 需要的 {@code List<Float>} 形态），响应不落库只记成功与否。必须走 {@code embedForResponse}：
     * {@code embed(String)} 把 metadata 丢在中间层、usage 取不到，报表里 embedding 一行永远是 0。 */
    public List<Float> embed(EmbeddingModel embeddingModel, ModelCallScope scope, String text) {
        // 加前缀把 embedding 与 chat 的 prompt 摘要在报表里隔开，同段文本两种调用的 hash 不撞
        return callRecorder.record(scope, embeddingModel, "embed:" + text, () -> {
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
            return new CallOutcome<>(toFloatList(response.getResult().getOutput()), usageOf(response));
        });
    }

    /** 批量向量化 — 整批一条供应商调用、一条记账日志（摄取侧一次 N 块），返回顺序与入参一致；失败整批抛出。 */
    public List<List<Float>> embedBatch(EmbeddingModel embeddingModel, ModelCallScope scope, List<String> texts) {
        return callRecorder.record(scope, embeddingModel, "embed-batch:" + texts.size() + " items", () -> {
            EmbeddingResponse response = embeddingModel.embedForResponse(texts);
            var vectors = new ArrayList<List<Float>>(texts.size());
            for (Embedding embedding : response.getResults()) {
                vectors.add(toFloatList(embedding.getOutput()));
            }
            return new CallOutcome<>(List.copyOf(vectors), usageOf(response));
        });
    }

    /** 多模态结构化输出：图片与「要哪些字段」同一次请求给到视觉模型。刻意不做「视觉模型写自由文本、文本模型再转
     * JSON」两段式 —— 第二次调用看不到图片，多付一次调用的钱与延迟，还丢掉没写进文字的画面细节。 */
    public <T> Optional<T> callJsonAsWithImages(
            ChatModel chatModel,
            ModelCallScope scope,
            String systemPrompt,
            String userText,
            List<String> imageUrls,
            Class<T> responseType) {
        Message userMessage = UserMessage.builder()
                .text(userText)
                .media(MediaResolver.of(imageUrls))
                .build();
        String json = callRecorder.record(
                scope,
                chatModel,
                systemPrompt + userText,
                () -> chatOutcome(chatModel.call(new Prompt(
                        List.of(new SystemMessage(systemPrompt), userMessage),
                        jsonOptions(chatModel, maxTokensOf(scope))))));
        return parseJson(scope, json, responseType);
    }

    /** 解析模型返回的 JSON；空内容或解析失败返回 empty（调用方据此降级，不打断业务链路）；不合 schema 与「模型不可用」同一结果，需要区分用 {@link #callJson}。 */
    private <T> Optional<T> parseJson(ModelCallScope scope, @Nullable String json, Class<T> responseType) {
        try {
            if (json == null || json.isBlank()) {
                log.warn("action=ai_json_empty, scope={}, message=模型返回空内容", scope);
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(json, responseType));
        } catch (Exception e) {
            log.warn("action=ai_json_unparsable, scope={}, reason={}", scope, e.getMessage());
            return Optional.empty();
        }
    }

    private static List<Message> systemUser(String systemPrompt, String userMessage) {
        return List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage));
    }

    private static String joinTexts(List<Message> messages) {
        return messages.stream().map(Message::getText).collect(Collectors.joining("\n"));
    }

    private static List<Float> toFloatList(float[] vector) {
        var list = new ArrayList<Float>(vector.length);
        for (float value : vector) {
            list.add(value);
        }
        return list;
    }

    /** 模型文本输出；可能没有结果（流式分片尤其常见只有 metadata 没有 output），统一回退空串。 */
    private static String outputText(@Nullable ChatResponse response) {
        var result = response == null ? null : response.getResult();
        return result == null || result.getOutput() == null
                ? ""
                : result.getOutput().getText();
    }

    private static CallOutcome<String> chatOutcome(@Nullable ChatResponse response) {
        return new CallOutcome<>(outputText(response), usageOf(response));
    }

    /** 模型请求的工具调用（可能为空：模型直接回了文本）；供应商未回结果同样为空。 */
    private static List<AssistantMessage.ToolCall> toolCallsOf(@Nullable ChatResponse response) {
        if (response == null
                || response.getResult() == null
                || response.getResult().getOutput() == null) {
            return List.of();
        }
        return response.getResult().getOutput().getToolCalls();
    }

    private static OpenAiChatOptions jsonOptions(ChatModel chatModel, @Nullable Integer maxTokens) {
        return options(chatModel, maxTokens)
                .responseFormat(OpenAiChatModel.ResponseFormat.builder()
                        .type(OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT)
                        .build())
                .build();
    }

    /** 工具决策请求选项 —— {@code tool_choice} 显式 {@code required}：ReAct 每轮的产物契约就是「一个工具调用」，
     * {@code auto} 下模型仍可能回纯文本，协议层要求后失败模式从「降级」变成「不可能发生」。 */
    private static OpenAiChatOptions toolOptions(
            ChatModel chatModel, List<ToolCallback> toolCallbacks, @Nullable Integer maxTokens) {
        return options(chatModel, maxTokens)
                .toolCallbacks(toolCallbacks)
                .toolChoice(TOOL_CHOICE_REQUIRED)
                .build();
    }

    /** scoped 调用的 per-request options —— 输出上限按场景预算真下发（{@code max_tokens}），与预算前置检查 / 记账共用同一份
     * 配置：供应商侧截断输出，最坏单次成本由此封顶；无配置返回 null 且不继承连接，避免只有连接字段的 options 把「没配」伪装成「配了」。 */
    private @Nullable OpenAiChatOptions scopedOptions(ChatModel chatModel, ModelCallScope scope) {
        Integer maxTokens = maxTokensOf(scope);
        return maxTokens == null ? null : options(chatModel, maxTokens).build();
    }

    /** per-request options 统一入口：先下发场景 {@code max_tokens} 再继承模型名 —— 只有这个顺序能让三种 options
     * 复用同一段模板。 */
    private static OpenAiChatOptions.Builder options(ChatModel chatModel, @Nullable Integer maxTokens) {
        var builder = OpenAiChatOptions.builder();
        if (maxTokens != null) {
            builder.maxTokens(maxTokens);
        }
        return inheritConnection(builder, chatModel);
    }

    /** 继承模型的连接与模型名 —— 只设业务字段时 {@code model} 为 null，openai-java 回退到 SDK 默认模型名（{@code
     * gpt-5-mini}），非 OpenAI 供应商直接 404、AI 决策点整体静默降级。 */
    private static OpenAiChatOptions.Builder inheritConnection(OpenAiChatOptions.Builder builder, ChatModel chatModel) {
        if (chatModel instanceof OpenAiChatModel openAiModel
                && openAiModel.getDefaultOptions() instanceof OpenAiChatOptions defaults) {
            builder.baseUrl(defaults.getBaseUrl()).apiKey(defaults.getApiKey()).model(defaults.getModel());
        }
        return builder;
    }

    /** 场景的单次调用输出上限；无配置或非正返回 null（不下发，保持供应商默认）。刻意不受 {@code budget.enabled} 控制：
     * 预算是记账口径，关掉只意味着不再累计，而 {@code max_tokens} 是供应商侧硬约束 —— 跟着关掉等于「省统计」变「放开成本」。 */
    private @Nullable Integer maxTokensOf(ModelCallScope scope) {
        var configured = aiProperties.budget().resolve(scope.budgetScenario());
        return configured != null && configured.maxTokensPerCall() > 0 ? configured.maxTokensPerCall() : null;
    }
}
