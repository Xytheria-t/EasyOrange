package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import com.cartethyia.easyorange.ai.application.service.AiModelSupport;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import java.util.concurrent.CompletableFuture;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/** 市场分析工具 — LLM 基于搜索结果价格总结市场行情（均价/性价比）；失败抛给管道判定降级。 */
@Component
public class MarketAnalysisTool implements SearchTool<String> {

    private static final String PROMPT_NAME = "search_market_system";

    private final ChatModel chatModel;
    private final AiModelSupport aiModelSupport;
    private final PromptRegistry promptRegistry;

    public MarketAnalysisTool(ChatModel chatModel, AiModelSupport aiModelSupport, PromptRegistry promptRegistry) {
        this.chatModel = chatModel;
        this.aiModelSupport = aiModelSupport;
        this.promptRegistry = promptRegistry;
    }

    @Override
    public String name() {
        return "market_analysis";
    }

    @Override
    public CompletableFuture<String> run(SearchToolContext context) {
        return CompletableFuture.supplyAsync(
                () -> aiModelSupport.callText(
                        chatModel,
                        AiCallScope.SEARCH_ENHANCE,
                        promptRegistry.require(PROMPT_NAME),
                        userMessage(context.marketContext())),
                VIRTUAL);
    }

    /** 召回结果里的资产标题由资产方填写，属不可信内容 —— 一并包进标签块。 */
    private static String userMessage(String marketContext) {
        return """
                <search_results>
                %s
                </search_results>
                """.formatted(marketContext);
    }
}
