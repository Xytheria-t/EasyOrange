package com.cartethyia.easyorange.ai.domain.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AI 调用场景 — 一次模型调用属于哪条业务链路，是限流、预算、缓存键三处的共同命名基准。
 * <p>
 * 场景名（枚举名小写）在三处必须一致，否则治理会各算各的：
 * <ul>
 *   <li>{@link #budgetScenario()} → {@code easyorange.ai.budget.scenarios} 的键 + {@code @TokenBudget(scenario=...)}</li>
 *   <li>{@link #rateLimitKeyPrefix()} → 令牌桶 key</li>
 *   <li>{@link #uriSuffix} → HTTP 入口到场景的映射（{@link #fromUri}）</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum AiCallScope {
    PRICING(10, "pricing"),
    REVIEW(10, "review"),
    COPY(20, "generate-copy"),
    AUTO_LISTING(5, "auto-listing"),
    SEMANTIC(30, "semantic-search"),
    QA(20, "qa"),
    SEARCH_ENHANCE(30, "search-enhance"),
    CHAT(20, "chat"),
    KNOWLEDGE(60, "knowledge");

    /** 每分钟限流额度（按用户维度；未登录按 IP）。 */
    private final int ratePerMinute;

    /** 限流拦截器的 URI 匹配片段。 */
    private final String uriSuffix;

    public static AiCallScope fromUri(String uri) {
        if (uri == null) return QA;
        for (var scope : values()) {
            if (uri.contains(scope.uriSuffix)) return scope;
        }
        return QA;
    }

    /**
     * 供应商故障时 stale 旧回答缓存的 key 前缀（{@code AiChatService} 的本地 Caffeine 缓存）。
     * <p>
     * 与语义缓存（Redis {@code eo:ai:semantic:*}）是两个独立的缓存，别名不同以免看混。
     */
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
