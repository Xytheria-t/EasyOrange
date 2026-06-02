package com.cartethyia.easyorange.ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AiCallScope {
    PRICING      (1, 3600, 10, "pricing"),
    REVIEW       (1, 3600, 10, "review"),
    COPY         (1, 3600, 20, "generate-copy"),
    AUTO_LISTING (1, 3600,  5, "auto-listing"),
    SEMANTIC     (1, 3600, 30, "semantic-search"),
    QA           (1,  900, 20, "qa");

    private final int version;
    private final int ttlSeconds;
    private final int ratePerMinute;
    private final String uriSuffix;

    public static AiCallScope fromUri(String uri) {
        if (uri == null) return QA;
        for (var scope : values()) {
            if (uri.contains(scope.uriSuffix)) return scope;
        }
        return QA;
    }

    public String cacheKeyPrefix() {
        return "ai:llm:v" + version + ":" + name().toLowerCase() + ":";
    }

    public String rateLimitKeyPrefix() {
        return "ai:rl:" + name().toLowerCase() + ":";
    }
}
