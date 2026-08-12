package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.service.AiModelSupport;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/** 建议问题工具 — LLM 基于用户需求生成 2-3 个追问，失败降级空列表。 */
@Slf4j
@Component
public class QuestionSuggestionTool implements SearchTool<List<String>> {

    private static final String SYSTEM_PROMPT = """
            基于用户需求和搜索结果，生成2-3个用户可能想追问的问题。
            每个问题不超过15个字。
            用逗号分隔输出，不要序号。
            """;

    private final ChatModel chatModel;
    private final AiModelSupport aiModelSupport;

    public QuestionSuggestionTool(ChatModel chatModel, AiModelSupport aiModelSupport) {
        this.chatModel = chatModel;
        this.aiModelSupport = aiModelSupport;
    }

    @Override
    public String name() {
        return "question_suggestion";
    }

    @Override
    public CompletableFuture<List<String>> run(SearchToolContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String result = aiModelSupport.callText(
                        chatModel, AiCallScope.SEARCH_ENHANCE, SYSTEM_PROMPT, context.keyword());
                return result != null ? Arrays.asList(result.split("[,，]")) : List.of();
            } catch (Exception e) {
                log.warn("Question suggestion tool failed", e);
                return List.of();
            }
        });
    }
}
