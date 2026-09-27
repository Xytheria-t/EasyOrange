package com.cartethyia.easyorange.ai.domain.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AI 调用场景 — 一次模型调用属于哪条业务链路，是限流、预算、缓存键三处的共同命名基准。
 * 场景名（枚举名小写）在三处必须一致，否则治理会各算各的：{@link #budgetScenario()} →
 * {@code easyorange.ai.budget.scenarios} 的键与 {@code @TokenBudget(scenario=...)}；
 * {@link #rateLimitKeyPrefix()} → 令牌桶 key；{@code uriSuffix} → HTTP 入口到场景的映射（{@link #fromUri}）。
 */
@Getter
@RequiredArgsConstructor
public enum AiCallScope {
    AUTO_LISTING(5, "auto-listing"),
    /**
     * 语义召回 — 商品搜索 {@code /api/products/search}（两路召回的 kNN 路）的检索词 embedding。
     * 该路径不在 {@code AiRateLimitInterceptor} 的 {@code /api/ai/**} 范围内，由框架
     * {@code RateLimitFilter} 统一限流；这一场景实际的治理面是预算与缓存键。
     */
    SEMANTIC(30, "products/search"),
    CHAT(20, "chat"),
    KNOWLEDGE(60, "knowledge");

    /** 每分钟限流额度（按用户维度；未登录按 IP）。 */
    private final int ratePerMinute;

    /** 限流拦截器的 URI 匹配片段。 */
    private final String uriSuffix;

    public static AiCallScope fromUri(String uri) {
        if (uri == null) return CHAT;
        for (var scope : values()) {
            if (uri.contains(scope.uriSuffix)) return scope;
        }
        return CHAT;
    }

    /** 供应商故障时 stale 旧回答缓存的 key 前缀（本地 Caffeine）—— 与语义缓存（Redis）是两个独立缓存，别名不同以免看混。 */
    public String cacheKeyPrefix() {
        return "ai:stale:" + name().toLowerCase() + ":";
    }

    public String rateLimitKeyPrefix() {
        return "ai:rl:" + name().toLowerCase() + ":";
    }

    /**
     * Token 预算场景键 —— 必须与 {@code easyorange.ai.budget.scenarios} 的键、{@code @TokenBudget(scenario=...)}
     * 的字面值一致，否则记账落在一个场景、限流检查读另一个场景，预算静默失效。
     */
    public String budgetScenario() {
        return name().toLowerCase();
    }
}
