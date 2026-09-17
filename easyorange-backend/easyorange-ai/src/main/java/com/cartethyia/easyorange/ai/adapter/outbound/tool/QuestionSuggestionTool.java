package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import com.cartethyia.easyorange.ai.application.service.AiModelSupport;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/** 建议问题工具 — LLM 基于用户需求生成 2-3 个追问；失败抛给管道判定降级。 */
@Component
public class QuestionSuggestionTool implements SearchTool<List<String>> {

    private static final String PROMPT_NAME = "search_question_suggestion_system";

    private final ChatModel chatModel;
    private final AiModelSupport aiModelSupport;
    private final PromptRegistry promptRegistry;

    public QuestionSuggestionTool(ChatModel chatModel, AiModelSupport aiModelSupport, PromptRegistry promptRegistry) {
        this.chatModel = chatModel;
        this.aiModelSupport = aiModelSupport;
        this.promptRegistry = promptRegistry;
    }

    @Override
    public String name() {
        return "question_suggestion";
    }

    @Override
    public CompletableFuture<List<String>> run(SearchToolContext context) {
        return CompletableFuture.supplyAsync(
                () -> {
                    String result = aiModelSupport.callText(
                            chatModel,
                            AiCallScope.SEARCH_ENHANCE,
                            promptRegistry.require(PROMPT_NAME),
                            userMessage(context.keyword()));
                    // 空串按逗号切出来是 [""]，会当成一条空问题渲染；空/空白一律降级为空列表
                    return result == null || result.isBlank() ? List.<String>of() : Arrays.asList(result.split("[,，]"));
                },
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
