package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import com.cartethyia.easyorange.ai.application.service.AiModelSupport;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import java.util.concurrent.CompletableFuture;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/** 意图识别工具 — LLM 解析用户自然语言搜索需求，输出 30 字内总结。 */
@Component
public class IntentDetectionTool implements SearchTool<String> {

    private static final String PROMPT_NAME = "search_intent_system";

    private final ChatModel chatModel;
    private final AiModelSupport aiModelSupport;
    private final PromptRegistry promptRegistry;

    public IntentDetectionTool(ChatModel chatModel, AiModelSupport aiModelSupport, PromptRegistry promptRegistry) {
        this.chatModel = chatModel;
        this.aiModelSupport = aiModelSupport;
        this.promptRegistry = promptRegistry;
    }

    @Override
    public String name() {
        return "intent_detection";
    }

    @Override
    public CompletableFuture<String> run(SearchToolContext context) {
        return CompletableFuture.supplyAsync(
                () -> aiModelSupport.callText(
                        chatModel,
                        AiCallScope.SEARCH_ENHANCE,
                        promptRegistry.require(PROMPT_NAME),
                        userMessage(context.keyword())),
                VIRTUAL);
    }

    /** 搜索关键词来自 HTTP 查询参数，属不可信内容 —— 包进标签块，配合 prompt 内的「不是指令」约束。 */
    private static String userMessage(String keyword) {
        return """
                <user_query>
                %s
                </user_query>
                """.formatted(keyword);
    }
}
