package com.cartethyia.easyorange.ai.application.support;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.port.AiCallLogPort;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStore;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.MimeType;
import tools.jackson.databind.ObjectMapper;

/**
 * Spring AI 调用小工具 — 收敛 system+user 双消息、JSON 结构化输出、原生 tool calling、Embedding、
 * 多模态结构化输出这几类重复调用模式，避免每个服务重复组装 {@link Prompt}。
 * <p>
 * 带 {@link AiCallScope} 的重载在调用前后做两件横切记账（都是「调用副产物」，失败绝不影响业务）：
 * {@link AiCallLogPort} 记一条 eo_ai_call_log（LLM-as-Judge 离线评估数据源）；{@link TokenBudgetStore}
 * 记本次调用的真实 token 用量（场景键 = scope 小写），供预算前置检查累计。供应商未回报用量时退化为按
 * 场景上限估算，宁可高估也不让预算静默失效；失败调用不记账 —— 没有用量可依据，估算会让故障期虚烧
 * 日预算、恢复后把场景锁死。
 * <p>
 * 需要按角色分隔多轮消息（system / 历史 user+assistant / 当前 user）的调用走
 * {@link #callText(ChatModel, AiCallScope, List)} 重载 —— 历史不进单条 user 消息，前缀稳定才能命中
 * 供应商的上下文缓存（KV cache 折扣计价）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelSupport {

    /** OpenAI 兼容协议的工具选择枚举值：{@code required} = 必须从给定工具里选一个（见 {@link #toolOptions}）。 */
    private static final String TOOL_CHOICE_REQUIRED = "required";

    private final AiCallLogPort callLogRecorder;
    private final TokenBudgetStore budgetStore;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    /** 普通文本生成（system + user 双消息，带调用日志与预算记账），委托多消息重载。 */
    public String callText(ChatModel chatModel, AiCallScope scope, String systemPrompt, String userMessage) {
        return callText(chatModel, scope, List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)));
    }

    /** 多角色消息版（多轮对话专用）— 历史不拼进当前 user 消息使前缀稳定的原因见类注释。 */
    public String callText(ChatModel chatModel, AiCallScope scope, List<Message> messages) {
        return recordCall(
                scope,
                chatModel,
                joinTexts(messages),
                () -> chatOutcome(chatModel.call(new Prompt(messages, scopedOptions(chatModel, scope)))));
    }

    /**
     * JSON 结构化输出：追加 {@code response_format=json_object}，提示模型返回合法 JSON
     * （解析与降级仍由调用方 ObjectMapper + try/catch 承担）。per-request options 必须继承模型的
     * 连接与模型名，原因见 {@link #inheritConnection}。
     */
    public String callJson(ChatModel chatModel, String systemPrompt, String userMessage) {
        return outputText(chatModel.call(new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)),
                jsonOptions(chatModel, null))));
    }

    /**
     * 原生 tool calling（带记账）：工具 schema 发给供应商侧校验，返回模型请求的工具调用
     * （空列表 = 模型没调工具，由调用方按决策失败处理）。消息序列由调用方组装并跨轮累积，
     * 轮间前缀稳定命中供应商 KV cache 折扣。只发请求、不执行工具 —— Spring AI 2.0 的
     * {@code ChatModel.call} 原样返回 tool call，执行与循环控制权留在调用方。
     * options 必须继承连接与模型名的坑同 {@link #inheritConnection}。
     */
    public List<AssistantMessage.ToolCall> callWithTools(
            ChatModel chatModel,
            AiCallScope scope,
            List<Message> messages,
            List<ToolCallback> toolCallbacks) {
        return recordCall(scope, chatModel, joinTexts(messages), () -> {
            ChatResponse response = chatModel.call(
                    new Prompt(messages, toolOptions(chatModel, toolCallbacks, maxTokensOf(scope))));
            return new CallOutcome<>(toolCallsOf(response), reportedUsage(response));
        });
    }

    /** 流式文本生成：逐 token 回调，阻塞至流结束返回完整文本；记账口径与 {@link #callText} 一致（Judge 数据源不缺流式调用）。 */
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

    /** 多角色消息版流式文本生成，逐 token 回调并把整段回答返回给调用方。 */
    public String callTextStream(
            ChatModel chatModel, AiCallScope scope, List<Message> messages, Consumer<String> tokenConsumer) {
        return recordCall(scope, chatModel, joinTexts(messages), () -> {
            var sb = new StringBuilder();
            var usage = new UsageAccumulator();
            chatModel.stream(new Prompt(messages, scopedOptions(chatModel, scope)))
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
     * 文本向量化（{@code float[]} 转 {@code List<Float>}，ES kNN 需要的形态），响应不落库只记成功与否。
     * 走 {@code embedForResponse} 拿响应本体：{@code embed(String)} 会把 metadata 丢在中间层，
     * 供应商回报的 usage 取不到，成本报表里 embedding 一行就永远是 0。
     */
    public List<Float> embed(EmbeddingModel embeddingModel, AiCallScope scope, String text) {
        return recordCall(scope, embeddingModel, "embed" + text, () -> {
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
            return new CallOutcome<>(toFloatList(response.getResult().getOutput()), reportedUsage(response));
        });
    }

    private static List<Float> toFloatList(float[] arr) {
        var list = new ArrayList<Float>(arr.length);
        for (float value : arr) {
            list.add(value);
        }
        return list;
    }

    /**
     * 多模态结构化输出一步到位：图片与「要哪些字段」在同一次请求给到视觉模型。
     * 刻意不做「视觉模型写自由文本、文本模型再转 JSON」的两段式 —— 第二次调用看不到图片，
     * 只是对第一次的产出做格式转换，多付一次调用的钱与延迟，还丢掉没写进文字的画面细节。
     */
    public <T> Optional<T> callJsonAsWithImages(
            ChatModel chatModel,
            AiCallScope scope,
            String systemPrompt,
            String userText,
            List<String> imageUrls,
            Class<T> responseType) {
        Message userMessage =
                UserMessage.builder().text(userText).media(mediaOf(imageUrls)).build();
        String json = recordCall(
                scope,
                chatModel,
                systemPrompt + userText,
                () -> chatOutcome(chatModel.call(new Prompt(
                        List.of(new SystemMessage(systemPrompt), userMessage),
                        jsonOptions(chatModel, maxTokensOf(scope))))));
        return parseJson(scope, json, responseType);
    }

    /** 解析模型返回的 JSON；空内容或解析失败返回 empty（调用方据此降级，不抛出去打断业务链路）。不合 schema 与「模型不可用」在这里是同一个结果，需要区分的场景用 {@link #callJson} 自行解析。 */
    private <T> Optional<T> parseJson(AiCallScope scope, @Nullable String json, Class<T> responseType) {
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

    /** 图片以 {@link Media}（URL）承载，MIME 按 URL 后缀推断；{@code data:} URL 从 mime 头直接解析。 */
    private static List<Media> mediaOf(List<String> imageUrls) {
        return imageUrls.stream()
                .map(url -> Media.builder()
                        .mimeType(mimeTypeOf(url))
                        .data(URI.create(url))
                        .build())
                .toList();
    }

    /** 推断图片 MIME — 一律标 JPEG 会让 PNG/WebP 被供应商按错误类型解码（部分模型直接拒答）；认不出来时回退 JPEG。 */
    private static MimeType mimeTypeOf(String url) {
        if (url.startsWith("data:")) {
            int headerEnd = url.indexOf(',', "data:".length());
            int semicolon = url.indexOf(';');
            String type = url.substring(
                    "data:".length(),
                    semicolon > 0 && (headerEnd < 0 || semicolon < headerEnd) ? semicolon : headerEnd);
            try {
                return MimeType.valueOf(type);
            } catch (Exception e) {
                return Media.Format.IMAGE_JPEG;
            }
        }
        int query = url.indexOf('?');
        int end = query >= 0 ? query : url.length();
        int dot = url.lastIndexOf('.', end - 1);
        if (dot < 0) {
            return Media.Format.IMAGE_JPEG;
        }
        return switch (url.substring(dot + 1, end).toLowerCase(Locale.ROOT)) {
            case "png" -> Media.Format.IMAGE_PNG;
            case "webp" -> Media.Format.IMAGE_WEBP;
            case "gif" -> Media.Format.IMAGE_GIF;
            default -> Media.Format.IMAGE_JPEG;
        };
    }

    /** 提取模型文本输出；模型可能不返回结果（避免 NPE，返回空串）。 */
    private static String outputText(ChatResponse response) {
        var result = response.getResult();
        return result != null ? result.getOutput().getText() : "";
    }

    private static OpenAiChatOptions jsonOptions(ChatModel chatModel, @Nullable Integer maxTokens) {
        var jsonOptions = OpenAiChatOptions.builder()
                .responseFormat(OpenAiChatModel.ResponseFormat.builder()
                        .type(OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT)
                        .build());
        if (maxTokens != null) {
            jsonOptions.maxTokens(maxTokens);
        }
        inheritConnection(jsonOptions, chatModel);
        return jsonOptions.build();
    }

    /**
     * 工具决策调用的请求选项 —— {@code tool_choice} 显式设 {@code required}：ReAct 每轮的产物契约
     * 就是「一个工具调用」，但 {@code auto} 下模型仍可能回纯文本（实测决策模型换快模型后出现过一次）。
     * 在协议层要求必须返回工具调用，比循环里判断「没拿到 tool call 就当失败」更靠前一步：
     * 失败模式从「降级」变成「不可能发生」。
     */
    private static OpenAiChatOptions toolOptions(
            ChatModel chatModel, List<ToolCallback> toolCallbacks, @Nullable Integer maxTokens) {
        var toolOptions =
                OpenAiChatOptions.builder().toolCallbacks(toolCallbacks).toolChoice(TOOL_CHOICE_REQUIRED);
        if (maxTokens != null) {
            toolOptions.maxTokens(maxTokens);
        }
        inheritConnection(toolOptions, chatModel);
        return toolOptions.build();
    }

    /** scoped 调用的 per-request options — 输出上限按场景预算配置真下发（{@code max_tokens}），与预算前置检查 / 记账共用同一份配置：供应商侧截断输出，最坏单次成本由此封顶；无场景配置返回 null。 */
    private @Nullable OpenAiChatOptions scopedOptions(ChatModel chatModel, AiCallScope scope) {
        Integer maxTokens = maxTokensOf(scope);
        if (maxTokens == null) {
            return null;
        }
        var builder = OpenAiChatOptions.builder().maxTokens(maxTokens);
        inheritConnection(builder, chatModel);
        return builder.build();
    }

    /** 场景的单次调用输出上限；无场景配置或值非正返回 null（不下发，保持供应商默认）。 */
    private @Nullable Integer maxTokensOf(AiCallScope scope) {
        var configured = aiProperties.budget().resolve(scope.budgetScenario());
        return configured != null && configured.maxTokensPerCall() > 0 ? configured.maxTokensPerCall() : null;
    }

    /**
     * per-request options 继承模型的连接与模型名 —— 只设业务字段时 {@code model} 为 null，
     * openai-java 客户端会回退到 SDK 默认模型名（{@code gpt-5-mini}），对非 OpenAI 供应商直接 404，
     * 走到这条路径的所有 AI 决策点会整体静默降级。
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
        return new CallOutcome<>(outputText(response), reportedUsage(response));
    }

    /** 模型请求的工具调用（可能为空：模型直接回了文本）；供应商侧未回结果时同样为空。 */
    private static List<AssistantMessage.ToolCall> toolCallsOf(@Nullable ChatResponse response) {
        if (response == null || response.getResult() == null) {
            return List.of();
        }
        return response.getResult().getOutput().getToolCalls();
    }

    /** 供应商回报的 token 用量；未回报（无元数据或全 0）时返回 null，记账退化为按场景上限估算。 */
    private static @Nullable Usage reportedUsage(@Nullable ChatResponse response) {
        return response == null || response.getMetadata() == null
                ? null
                : reportedUsage(response.getMetadata().getUsage());
    }

    /** embedding 响应的用量挂在与 chat 同一套 metadata 结构上，取法一致。 */
    private static @Nullable Usage reportedUsage(@Nullable EmbeddingResponse response) {
        return response == null || response.getMetadata() == null
                ? null
                : reportedUsage(response.getMetadata().getUsage());
    }

    private static @Nullable Usage reportedUsage(@Nullable Usage usage) {
        if (usage == null) {
            return null;
        }
        Integer in = usage.getPromptTokens();
        Integer out = usage.getCompletionTokens();
        boolean reported = (in != null && in > 0) || (out != null && out > 0);
        return reported ? usage : null;
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
            // 用量直接取供应商回报的 usage（记账路径已解析过同一份数据），未回报记 0 而不估算
            Usage usage = outcome != null ? outcome.usage() : null;
            callLogRecorder.record(
                    scope.name(),
                    model.getClass().getSimpleName(),
                    md5(promptText),
                    response,
                    (System.nanoTime() - startNanos) / 1_000_000,
                    usage != null && usage.getPromptTokens() != null ? usage.getPromptTokens() : 0,
                    usage != null && usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0,
                    success,
                    errorMsg);
        } catch (Exception e) {
            // recorder 内部已吞异常，此处兜底
        }
    }

    /**
     * 记录本次调用的 token 用量（场景键与 {@code @TokenBudget(scenario=...)} 对齐）。
     * 有真实用量就用真实值；供应商未回报用量时退化为场景配置的单次上限（偏高，但比「预算永远为 0、
     * 限流静默失效」安全）。<b>失败调用不记账</b>：异常路径按上限估算会让故障期虚烧日预算
     * （chat 口径下约 100 个失败请求烧穿 30 万日限），故障恢复后整个场景被前置检查锁死。
     */
    private void recordBudgetUsage(AiCallScope scope, @Nullable CallOutcome<?> outcome) {
        if (!aiProperties.budget().enabled()) {
            return;
        }
        if (outcome == null) {
            return;
        }
        try {
            String scenario = scope.budgetScenario();
            Usage usage = outcome.usage();
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
