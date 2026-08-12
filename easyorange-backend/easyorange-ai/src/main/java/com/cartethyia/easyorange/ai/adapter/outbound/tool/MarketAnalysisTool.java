package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.service.AiModelSupport;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/** 市场分析工具 — LLM 基于搜索结果价格总结市场行情（均价/性价比），失败降级 null。 */
@Slf4j
@Component
public class MarketAnalysisTool implements SearchTool<String> {

    private static final String SYSTEM_PROMPT = """
            你是 EasyOrange — AI 工程化 的市场分析助手。根据搜索到的资产价格信息，
            用一句话概括当前市场价格情况（如均价、性价比等），不超过40个字。
            直接输出分析结果，不要前缀。
            """;

    private final ChatModel chatModel;
    private final AiModelSupport aiModelSupport;

    public MarketAnalysisTool(ChatModel chatModel, AiModelSupport aiModelSupport) {
        this.chatModel = chatModel;
        this.aiModelSupport = aiModelSupport;
    }

    @Override
    public String name() {
        return "market_analysis";
    }

    @Override
    public CompletableFuture<String> run(SearchToolContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return aiModelSupport.callText(
                        chatModel, AiCallScope.SEARCH_ENHANCE, SYSTEM_PROMPT, context.marketContext());
            } catch (Exception e) {
                log.warn("Market analysis tool failed", e);
                return null;
            }
        });
    }
}
