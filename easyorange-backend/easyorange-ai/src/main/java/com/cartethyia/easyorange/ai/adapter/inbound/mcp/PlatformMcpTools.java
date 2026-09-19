package com.cartethyia.easyorange.ai.adapter.inbound.mcp;

import com.cartethyia.easyorange.ai.application.service.AssetSourcingService;
import com.cartethyia.easyorange.ai.application.service.KnowledgeRetrievalService;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.CategorySummary;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.port.AssetDetailPort;
import com.cartethyia.easyorange.ai.domain.port.CategoryListPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP 公开只读工具面 — 外部 MCP client（Cursor / Claude Desktop 等）经 streamable HTTP
 * （端点 {@code /mcp}）调用的入口。
 * <p>
 * 与 Agent 内部工具（{@code AgentLoopRunner} 工具面）是<b>两级独立暴露</b>：外部 client
 * 无用户上下文，这里只挂公开只读数据（在售资产检索/详情、类目、平台规则知识），
 * 不暴露订单、个人信息与任何写路径（信任边界见根 AGENTS.md）。
 * <p>
 * 降级语义沿用服务层：检索类调用失败返回空列表不抛异常（与对话主链路同一取向），
 * 详情查询的底层故障按 MCP 协议转为错误结果交 client 处理。
 * 每次调用计 {@code easyorange.mcp.tool{name}} 指标，与 Agent 循环指标同面板观测。
 */
@Component
@RequiredArgsConstructor
public class PlatformMcpTools {

    static final String TOOL_SEARCH_PRODUCTS = "search_products";
    static final String TOOL_GET_PRODUCT_DETAIL = "get_product_detail";
    static final String TOOL_LIST_CATEGORIES = "list_categories";
    static final String TOOL_SEARCH_KNOWLEDGE = "search_platform_knowledge";

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_PRODUCT_TOP_K = 20;
    private static final int MAX_KNOWLEDGE_TOP_K = 10;

    private final AssetSourcingService assetSourcingService;
    private final AssetDetailPort assetDetailPort;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final CategoryListPort categoryListPort;
    private final MeterRegistry meterRegistry;

    @McpTool(
            name = TOOL_SEARCH_PRODUCTS,
            description =
                    "按关键词检索 EasyOrange 平台在售二手资产。支持自然语言描述（如「九成新的显卡」），" + "返回资产 ID、标题、价格、类目与成色。结果为空说明无在售匹配，可换更宽泛的关键词重试。")
    public List<AssetHit> searchProducts(
            @McpToolParam(description = "检索关键词，支持中文自然语言") String query,
            @McpToolParam(description = "返回数量上限，默认 5，最大 20", required = false) @Nullable Integer topK) {
        recordCall(TOOL_SEARCH_PRODUCTS);
        return assetSourcingService.search(query, clampTopK(topK, MAX_PRODUCT_TOP_K));
    }

    @McpTool(
            name = TOOL_GET_PRODUCT_DETAIL,
            description = "按资产 ID 查询在售资产详情（描述、成色、所在地、卖家、在售状态）。" + "found=false 表示资产不存在或已下架。")
    public ProductDetailResult getProductDetail(@McpToolParam(description = "资产 ID（36 位 UUID）") String productId) {
        recordCall(TOOL_GET_PRODUCT_DETAIL);
        if (productId == null || productId.isBlank()) {
            return new ProductDetailResult(false, null);
        }
        return assetDetailPort
                .findDetail(productId.trim())
                .map(detail -> new ProductDetailResult(true, detail))
                .orElseGet(() -> new ProductDetailResult(false, null));
    }

    @McpTool(
            name = TOOL_LIST_CATEGORIES,
            description = "浏览平台资产类目。不传 parentId 返回一级类目列表（含各类目在售资产数）；" + "传 parentId 返回其直接子类目。")
    public List<CategorySummary> listCategories(
            @McpToolParam(description = "父类目 ID；省略时返回一级类目", required = false) @Nullable String parentId) {
        recordCall(TOOL_LIST_CATEGORIES);
        return categoryListPort.list(parentId);
    }

    @McpTool(
            name = TOOL_SEARCH_KNOWLEDGE,
            description = "检索平台规则知识库（交易流程、担保支付、退换与纠纷规则等）。" + "回答「平台怎么用」类问题时先查这里，引用时附上文档标题。")
    public List<KnowledgeHit> searchPlatformKnowledge(
            @McpToolParam(description = "检索关键词，支持中文自然语言") String query,
            @McpToolParam(description = "返回条数上限，默认 5，最大 10", required = false) @Nullable Integer topK) {
        recordCall(TOOL_SEARCH_KNOWLEDGE);
        return knowledgeRetrievalService.search(query, clampTopK(topK, MAX_KNOWLEDGE_TOP_K));
    }

    private void recordCall(String tool) {
        meterRegistry.counter("easyorange.mcp.tool", "name", tool).increment();
    }

    private static int clampTopK(@Nullable Integer topK, int max) {
        return topK == null ? DEFAULT_TOP_K : Math.max(1, Math.min(topK, max));
    }
}
