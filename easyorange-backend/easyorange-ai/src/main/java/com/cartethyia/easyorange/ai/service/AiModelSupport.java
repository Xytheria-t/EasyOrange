package com.cartethyia.easyorange.ai.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * Spring AI 调用小工具 — 收敛 system+user 双消息、JSON 结构化输出、Embedding、
 * 多图视觉识别这几类重复调用模式，避免每个服务重复组装 {@link Prompt}。
 * <p>
 * 纯静态无状态工具，不构成端口/适配器抽象：服务直接注入
 * {@link ChatModel}/{@link EmbeddingModel}（Spring AI 框架 bean），
 * 这里只做调用编排的代码去重。
 */
public final class AiModelSupport {

    private AiModelSupport() {}

    /**
     * 普通文本生成：system + user 双消息。
     */
    public static String callText(ChatModel chatModel, String systemPrompt, String userMessage) {
        return chatModel
                .call(new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage))))
                .getResult()
                .getOutput()
                .getText();
    }

    /**
     * JSON 结构化输出：在 system + user 双消息之上追加 {@code response_format=json_object}，
     * 提示模型返回合法 JSON（解析与降级仍由调用方 ObjectMapper + try/catch 承担）。
     */
    public static String callJson(ChatModel chatModel, String systemPrompt, String userMessage) {
        var jsonOptions = OpenAiChatOptions.builder()
                .responseFormat(OpenAiChatModel.ResponseFormat.builder()
                        .type(OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT)
                        .build())
                .build();
        return chatModel
                .call(new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)), jsonOptions))
                .getResult()
                .getOutput()
                .getText();
    }

    /**
     * 文本向量化：{@code float[]} 转 {@code List<Float>}（ES kNN 查询需要的形态）。
     */
    public static List<Float> embed(EmbeddingModel embeddingModel, String text) {
        float[] arr = embeddingModel.embed(text);
        var list = new ArrayList<Float>(arr.length);
        for (float value : arr) {
            list.add(value);
        }
        return list;
    }

    /**
     * 多图视觉识别：图片以 {@link Media}（URL）随提示词一并交给视觉模型。
     */
    public static String analyzeImages(ChatModel visionChatModel, List<String> imageUrls, String prompt) {
        List<Media> media = imageUrls.stream()
                .map(url -> Media.builder()
                        .mimeType(Media.Format.IMAGE_JPEG)
                        .data(URI.create(url))
                        .build())
                .toList();
        Message userMessage = UserMessage.builder().text(prompt).media(media).build();
        return visionChatModel
                .call(new Prompt(userMessage))
                .getResult()
                .getOutput()
                .getText();
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
}
