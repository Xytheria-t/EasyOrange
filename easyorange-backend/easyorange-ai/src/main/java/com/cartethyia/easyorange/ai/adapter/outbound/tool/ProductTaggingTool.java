package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import com.cartethyia.easyorange.ai.service.ProductTagger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/** 商品标签工具 — 本地规则引擎（折扣/图片/信用分），零 LLM 调用，亚毫秒响应。 */
@Component
public class ProductTaggingTool implements SearchTool<Map<String, List<String>>> {

    private final ProductTagger productTagger;

    public ProductTaggingTool(ProductTagger productTagger) {
        this.productTagger = productTagger;
    }

    @Override
    public String name() {
        return "product_tagging";
    }

    @Override
    public String description() {
        return "基于折扣/实拍图/资产方信用分对 Top 商品打标签（本地规则引擎，零 LLM 成本）";
    }

    @Override
    public SearchToolKind kind() {
        return SearchToolKind.RULE;
    }

    @Override
    public CompletableFuture<Map<String, List<String>>> run(SearchToolContext context) {
        return CompletableFuture.supplyAsync(() -> productTagger.tagProducts(context.topProducts()));
    }
}
