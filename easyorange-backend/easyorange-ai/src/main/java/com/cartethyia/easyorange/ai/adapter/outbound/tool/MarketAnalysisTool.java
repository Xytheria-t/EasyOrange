package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import com.cartethyia.easyorange.ai.domain.model.PriceStats;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/**
 * 市场分析工具 — 规则计算（在售件数 / 均价 / 价格区间），零 LLM 调用。
 * <p>
 * 这一路原先把召回结果的价格文本交给模型「分析」，但那份文本本身就是编排器用商品价格拼出来的
 * —— 让模型对已经算好的数字再做一次算术，既多付一次供应商调用，又多一层算错的可能；
 * 而供应商变慢时这两路 LLM 工具还会恒定超时缺席，又因「降级不写缓存」被每次搜索重算一遍。
 * 改为直接计算后，单次自然语言搜索的模型调用从 3 路降到 1 路（只剩意图识别）。
 * <p>
 * 口径与文案复用 {@link PriceStats}（agent 侧 market_price_stats 工具同一份实现）。
 * <p>
 * 仍走 {@code supplyAsync(VIRTUAL)} 而不是 {@code completedFuture}：规则工具的异常也必须落进
 * future，否则会在编排器 try 块之外同步抛出，让「单路失败不拖垮整体」的降级约定失效。
 */
@Component
public class MarketAnalysisTool implements SearchTool<String> {

    /** 工具名 —— 编排器按它取用，故此处是唯一定义处（见 SearchToolRegistry）。 */
    public static final String NAME = "market_analysis";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CompletableFuture<String> run(SearchToolContext context) {
        return CompletableFuture.supplyAsync(() -> summarize(context.topProducts()), VIRTUAL);
    }

    /** 无有效价格时返回 null —— 与管道「本轮无结果」语义一致，前端按 falsy 跳过渲染。 */
    private static String summarize(List<ProductReadModel> products) {
        return PriceStats.ofPrices(products.stream().map(ProductReadModel::price).toList())
                .map(PriceStats::observation)
                .orElse(null);
    }
}
