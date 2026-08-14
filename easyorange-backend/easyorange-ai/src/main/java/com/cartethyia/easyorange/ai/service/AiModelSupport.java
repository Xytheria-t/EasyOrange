package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.adapter.outbound.AiCallLogRecorder;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
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
 * 带 {@link AiCallScope} 的重载在调用前后经 {@link AiCallLogRecorder}
 * 记录一条 eo_ai_call_log（LLM-as-Judge 离线评估数据源）。
 */
@Component
@RequiredArgsConstructor
public class AiModelSupport {

    private final AiCallLogRecorder callLogRecorder;

    /**
     * 普通文本生成：system + user 双消息。
     */
    public String callText(ChatModel chatModel, String systemPrompt, String userMessage) {
        return outputText(
                chatModel.call(new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)))));
    }

    /**
     * 普通文本生成（带调用日志）：system + user 双消息，成功后记录 scope/model/耗时。
     */
    public String callText(ChatModel chatModel, AiCallScope scope, String systemPrompt, String userMessage) {
        return recordCall(
                scope, chatModel, systemPrompt, userMessage, () -> callText(chatModel, systemPrompt, userMessage));
    }

    /**
     * JSON 结构化输出：在 system + user 双消息之上追加 {@code response_format=json_object}，
     * 提示模型返回合法 JSON（解析与降级仍由调用方 ObjectMapper + try/catch 承担）。
     */
    public String callJson(ChatModel chatModel, String systemPrompt, String userMessage) {
        var jsonOptions = OpenAiChatOptions.builder()
                .responseFormat(OpenAiChatModel.ResponseFormat.builder()
                        .type(OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT)
                        .build())
                .build();
        return outputText(chatModel.call(
                new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)), jsonOptions)));
    }

    /**
     * JSON 结构化输出（带调用日志）：同 {@link #callJson}，记录 scope/model/耗时。
     */
    public String callJson(ChatModel chatModel, AiCallScope scope, String systemPrompt, String userMessage) {
        return recordCall(
                scope, chatModel, systemPrompt, userMessage, () -> callJson(chatModel, systemPrompt, userMessage));
    }

    /**
     * 流式文本生成（带调用日志）：逐 token 回调 {@code tokenConsumer}，阻塞至流结束返回完整文本。
     * <p>
     * 供 SSE 场景使用（AiChatService 把 token 回调接到 SseEmitter）；调用日志/耗时统计
     * 与 {@link #callText} 一致，落库的 response_text 是完整拼接结果（Judge 数据源不缺流式调用）。
     */
    public String callTextStream(
            ChatModel chatModel,
            AiCallScope scope,
            String systemPrompt,
            String userMessage,
            Consumer<String> tokenConsumer) {
        return recordCall(scope, chatModel, systemPrompt, userMessage, () -> {
            var sb = new StringBuilder();
            chatModel.stream(new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage))))
                    .doOnNext(response -> {
                        String token = outputText(response);
                        if (token != null && !token.isEmpty()) {
                            sb.append(token);
                            tokenConsumer.accept(token);
                        }
                    })
                    .blockLast();
            return sb.toString();
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
     * 文本向量化（带调用日志）：同 {@link #embed}，记录 scope/耗时（响应不落库，只记成功与否）。
     */
    public List<Float> embed(EmbeddingModel embeddingModel, AiCallScope scope, String text) {
        return recordCall(scope, embeddingModel, "embed", text, () -> embed(embeddingModel, text));
    }

    /**
     * 多图视觉识别：图片以 {@link Media}（URL）随提示词一并交给视觉模型。
     */
    public String analyzeImages(ChatModel visionChatModel, List<String> imageUrls, String prompt) {
        List<Media> media = imageUrls.stream()
                .map(url -> Media.builder()
                        .mimeType(Media.Format.IMAGE_JPEG)
                        .data(URI.create(url))
                        .build())
                .toList();
        Message userMessage = UserMessage.builder().text(prompt).media(media).build();
        return outputText(visionChatModel.call(new Prompt(userMessage)));
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

    private <T> T recordCall(
            AiCallScope scope, Object model, String systemPrompt, String userMessage, Supplier<T> supplier) {
        long start = System.nanoTime();
        T result = null;
        boolean success = false;
        String errorMsg = null;
        try {
            result = supplier.get();
            success = true;
            return result;
        } catch (Exception e) {
            errorMsg = e.getMessage();
            throw e;
        } finally {
            record(
                    scope,
                    model,
                    systemPrompt,
                    userMessage,
                    result instanceof String s ? s : null,
                    start,
                    success,
                    errorMsg);
        }
    }

    private void record(
            AiCallScope scope,
            Object model,
            String systemPrompt,
            String userMessage,
            String response,
            long startNanos,
            boolean success,
            String errorMsg) {
        try {
            callLogRecorder.record(
                    scope.name(),
                    model.getClass().getSimpleName(),
                    md5(systemPrompt + userMessage),
                    response,
                    (System.nanoTime() - startNanos) / 1_000_000,
                    success,
                    errorMsg);
        } catch (Exception e) {
            // recorder 内部已吞异常，此处兜底
        }
    }

    private static String md5(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }
}
