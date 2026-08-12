package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.service.AiModelSupport;
import java.util.concurrent.CompletableFuture;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/** 意图识别工具 — LLM 解析用户自然语言搜索需求，输出 30 字内总结。 */
@Component
public class IntentDetectionTool implements SearchTool<String> {

    private static final String SYSTEM_PROMPT = """
            你是 EasyOrange — AI 工程化 的 AI 导购助手。
            用户输入了一段自然语言商品搜索需求。
            请用一句简洁的话总结用户想找什么，不超过30个字。
            直接输出总结，不要前缀。
            示例: "想找5000以内适合编程的笔记本"
            """;

    private final ChatModel chatModel;
    private final AiModelSupport aiModelSupport;

    public IntentDetectionTool(ChatModel chatModel, AiModelSupport aiModelSupport) {
        this.chatModel = chatModel;
        this.aiModelSupport = aiModelSupport;
    }

    @Override
    public String name() {
        return "intent_detection";
    }

    @Override
    public CompletableFuture<String> run(SearchToolContext context) {
        return CompletableFuture.supplyAsync(
                () -> aiModelSupport.callText(chatModel, AiCallScope.SEARCH_ENHANCE, SYSTEM_PROMPT, context.keyword()));
    }
}
