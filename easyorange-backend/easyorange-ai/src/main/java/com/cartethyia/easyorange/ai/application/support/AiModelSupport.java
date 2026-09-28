package com.cartethyia.easyorange.ai.application.support;

import static com.cartethyia.easyorange.ai.application.support.AiCallRecorder.UsageAccumulator;
import static com.cartethyia.easyorange.ai.application.support.AiCallRecorder.usageOf;

import com.cartethyia.easyorange.ai.application.support.AiCallRecorder.CallOutcome;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
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
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Spring AI 调用小工具 — 收敛 system+user 双消息、JSON 结构化输出、原生 tool calling、Embedding、
 * 多模态结构化输出这几类重复调用模式，避免每个服务重复组装 {@link Prompt}。
 * <p>
 * 带 {@link AiCallScope} 的方法一律经 {@link AiCallRecorder} 执行，由它落 eo_ai_call_log 与场景预算
 * 记账 —— 本类不直接碰两个记账端口，治理出口因此只有一个。唯一的例外是 {@link #callJson}：它专供
 * LLM-as-Judge 的离线评估调用，刻意不记账（见该方法）。
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

    private final AiCallRecorder callRecorder;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    /** 普通文本生成（system + user 双消息，带调用日志与预算记账）。 */
    public String callText(ChatModel chatModel, AiCallScope scope, String systemPrompt, String userMessage) {
        return callText(chatModel, scope, systemUser(systemPrompt, userMessage));
    }

    /** 多角色消息版（多轮对话专用）—— 历史不拼进当前 user 消息、使前缀稳定命中 KV cache 折扣的原因见类注释。 */
    public String callText(ChatModel chatModel, AiCallScope scope, List<Message> messages) {
        return callRecorder.record(
                scope,
                chatModel,
                joinTexts(messages),
                () -> chatOutcome(chatModel.call(new Prompt(messages, scopedOptions(chatModel, scope)))));
    }

    /**
     * JSON 结构化输出（<b>唯一不记账的调用</b>）：追加 {@code response_format=json_object}，提示模型返回
     * 合法 JSON，解析与降级仍由调用方 ObjectMapper + try/catch 承担。
     * <p>
     * 不走 {@link AiCallRecorder} 是刻意的：当前唯一调用方是 {@code AiJudge}，其调用只发生在金标准集
     * 回归跑批里，给它记账会让「谁来评估评估者」在成本报表里套娃，还要额外给评估场景配预算。
     * 换句话说这里省的是离线评估自身的成本，不是业务成本 —— 走这条路的业务调用必须换成带 scope 的重载。
     */
    public String callJson(ChatModel chatModel, String systemPrompt, String userMessage) {
        return outputText(
                chatModel.call(new Prompt(systemUser(systemPrompt, userMessage), jsonOptions(chatModel, null))));
    }

    /**
     * 原生 tool calling（带记账）：工具 schema 发给供应商侧校验，返回模型请求的工具调用
     * （空列表 = 模型没调工具，由调用方按决策失败处理）。消息序列由调用方组装并跨轮累积，
     * 轮间前缀稳定命中供应商 KV cache 折扣。只发请求、不执行工具 —— Spring AI 2.0 的
     * {@code ChatModel.call} 原样返回 tool call，执行与循环控制权留在调用方。
     */
    public List<AssistantMessage.ToolCall> callWithTools(
            ChatModel chatModel, AiCallScope scope, List<Message> messages, List<ToolCallback> toolCallbacks) {
        return callRecorder.record(scope, chatModel, joinTexts(messages), () -> {
            ChatResponse response =
                    chatModel.call(new Prompt(messages, toolOptions(chatModel, toolCallbacks, maxTokensOf(scope))));
            return new CallOutcome<>(toolCallsOf(response), usageOf(response));
        });
    }

    /** 流式文本生成：逐 token 回调，阻塞至流结束返回完整文本；记账口径与 {@link #callText} 一致（Judge 数据源不缺流式调用）。 */
    public String callTextStream(
            ChatModel chatModel,
            AiCallScope scope,
            String systemPrompt,
            String userMessage,
            Consumer<String> tokenConsumer) {
        return callTextStream(chatModel, scope, systemUser(systemPrompt, userMessage), tokenConsumer);
    }

    /** 多角色消息版流式文本生成，逐 token 回调并把整段回答返回给调用方。 */
    public String callTextStream(
            ChatModel chatModel, AiCallScope scope, List<Message> messages, Consumer<String> tokenConsumer) {
        return callRecorder.record(scope, chatModel, joinTexts(messages), () -> {
            var collected = new StringBuilder();
            var usage = new UsageAccumulator();
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
            return new CallOutcome<>(collected.toString(), usage.result());
        });
    }

    /**
     * 文本向量化（{@code float[]} 转 {@code List<Float>}，ES kNN 需要的形态），响应不落库只记成功与否。
     * 走 {@code embedForResponse} 拿响应本体：{@code embed(String)} 会把 metadata 丢在中间层，
     * 供应商回报的 usage 取不到，成本报表里 embedding 一行就永远是 0。
     */
    public List<Float> embed(EmbeddingModel embeddingModel, AiCallScope scope, String text) {
        // 加前缀把 embedding 与 chat 的 prompt 摘要在报表里隔开，同段文本两种调用的 hash 不撞
        return callRecorder.record(scope, embeddingModel, "embed:" + text, () -> {
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
            return new CallOutcome<>(toFloatList(response.getResult().getOutput()), usageOf(response));
        });
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

    /** 提取模型文本输出；模型可能不返回结果（流式分片尤其常见只有 metadata 没有 output），统一回退空串。 */
    private static String outputText(@Nullable ChatResponse response) {
        var result = response == null ? null : response.getResult();
        return result == null || result.getOutput() == null
                ? ""
                : result.getOutput().getText();
    }

    /** 把 ChatResponse 包成「文本 + 用量」；响应为 null 时是空文本、无用量。 */
    private static CallOutcome<String> chatOutcome(@Nullable ChatResponse response) {
        return new CallOutcome<>(outputText(response), usageOf(response));
    }

    /** 模型请求的工具调用（可能为空：模型直接回了文本）；供应商侧未回结果时同样为空。 */
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

    /**
     * 工具决策调用的请求选项 —— {@code tool_choice} 显式设 {@code required}：ReAct 每轮的产物契约
     * 就是「一个工具调用」，但 {@code auto} 下模型仍可能回纯文本（实测决策模型换快模型后出现过一次）。
     * 在协议层要求必须返回工具调用，比循环里判断「没拿到 tool call 就当失败」更靠前一步：
     * 失败模式从「降级」变成「不可能发生」。
     */
    private static OpenAiChatOptions toolOptions(
            ChatModel chatModel, List<ToolCallback> toolCallbacks, @Nullable Integer maxTokens) {
        return options(chatModel, maxTokens)
                .toolCallbacks(toolCallbacks)
                .toolChoice(TOOL_CHOICE_REQUIRED)
                .build();
    }

    /**
     * scoped 调用的 per-request options —— 输出上限按场景预算配置真下发（{@code max_tokens}），
     * 与预算前置检查 / 记账共用同一份配置：供应商侧截断输出，最坏单次成本由此封顶。
     * 无场景配置返回 null（不下发，保持供应商默认），此时连继承连接都不做，避免发一个只有
     * 连接字段的 options 把「没配」这件事伪装成「配了」。
     */
    private @Nullable OpenAiChatOptions scopedOptions(ChatModel chatModel, AiCallScope scope) {
        Integer maxTokens = maxTokensOf(scope);
        return maxTokens == null ? null : options(chatModel, maxTokens).build();
    }

    /**
     * per-request options 的统一入口：先按场景下发的 {@code max_tokens}，再继承连接的模型名。
     * 顺序不能反 —— 继承会写 model，max_tokens 只是业务字段，两者互不覆盖，但只有「先定上限再补连接」
     * 这一种顺序能让三种 options 复用同一段模板。
     */
    private static OpenAiChatOptions.Builder options(ChatModel chatModel, @Nullable Integer maxTokens) {
        var builder = OpenAiChatOptions.builder();
        if (maxTokens != null) {
            builder.maxTokens(maxTokens);
        }
        return inheritConnection(builder, chatModel);
    }

    /**
     * 继承模型的连接与模型名 —— 只设业务字段时 {@code model} 为 null，openai-java 客户端会回退到
     * SDK 默认模型名（{@code gpt-5-mini}），对非 OpenAI 供应商直接 404，走到这条路径的所有 AI 决策点
     * 会整体静默降级。
     */
    private static OpenAiChatOptions.Builder inheritConnection(OpenAiChatOptions.Builder builder, ChatModel chatModel) {
        if (chatModel instanceof OpenAiChatModel openAiModel
                && openAiModel.getDefaultOptions() instanceof OpenAiChatOptions defaults) {
            builder.baseUrl(defaults.getBaseUrl()).apiKey(defaults.getApiKey()).model(defaults.getModel());
        }
        return builder;
    }

    /**
     * 场景的单次调用输出上限；无场景配置或值非正返回 null（不下发，保持供应商默认）。
     * <p>
     * <b>刻意不受 {@code budget.enabled} 开关控制</b>：预算是记账口径，关掉只意味着不再累计，
     * 而 {@code max_tokens} 是供应商侧的硬约束 —— 跟着一起关掉等于把「省统计」变成「放开单次成本」。
     */
    private @Nullable Integer maxTokensOf(AiCallScope scope) {
        var configured = aiProperties.budget().resolve(scope.budgetScenario());
        return configured != null && configured.maxTokensPerCall() > 0 ? configured.maxTokensPerCall() : null;
    }
}
