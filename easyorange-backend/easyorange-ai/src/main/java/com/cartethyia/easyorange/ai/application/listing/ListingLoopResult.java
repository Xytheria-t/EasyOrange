package com.cartethyia.easyorange.ai.application.listing;

import com.cartethyia.easyorange.ai.application.toolcall.ToolCallLoopOutcome;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import java.util.List;

/**
 * 发布链路循环结果 — 召回物供最终生成拼观察块（行情由生成侧按累加器现算，不入记录）；outcome / rounds
 * 供指标与降级归因；toolPath 供路由评估对照金标准集。与买家侧 {@code ToolCallLoop.Result} 同构，但
 * 发布链路没有 product_detail / 偏好，故无详情列表。
 */
public record ListingLoopResult(
        List<KnowledgeHit> knowledgeHits,
        List<AssetHit> assetHits,
        ToolCallLoopOutcome outcome,
        int rounds,
        List<String> toolPath) {}
