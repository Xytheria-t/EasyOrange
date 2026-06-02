package com.cartethyia.easyorange.ai.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AiCallScope 测试")
class AiCallScopeTest {

    @Test
    @DisplayName("fromUri 匹配 pricing")
    void fromUri_pricing() {
        assertThat(AiCallScope.fromUri("/api/ai/pricing")).isEqualTo(AiCallScope.PRICING);
    }

    @Test
    @DisplayName("fromUri 匹配 review")
    void fromUri_review() {
        assertThat(AiCallScope.fromUri("/api/ai/review")).isEqualTo(AiCallScope.REVIEW);
    }

    @Test
    @DisplayName("fromUri 匹配 auto-listing")
    void fromUri_autoListing() {
        assertThat(AiCallScope.fromUri("/api/ai/auto-listing")).isEqualTo(AiCallScope.AUTO_LISTING);
    }

    @Test
    @DisplayName("fromUri 匹配 qa")
    void fromUri_qa() {
        assertThat(AiCallScope.fromUri("/api/ai/qa")).isEqualTo(AiCallScope.QA);
    }

    @Test
    @DisplayName("fromUri 匹配 semantic-search")
    void fromUri_semantic() {
        assertThat(AiCallScope.fromUri("/api/ai/semantic-search")).isEqualTo(AiCallScope.SEMANTIC);
    }

    @Test
    @DisplayName("fromUri 匹配 generate-copy")
    void fromUri_copy() {
        assertThat(AiCallScope.fromUri("/api/ai/generate-copy")).isEqualTo(AiCallScope.COPY);
    }

    @Test
    @DisplayName("fromUri 未匹配返回 QA")
    void fromUri_unknown() {
        assertThat(AiCallScope.fromUri("/api/ai/unknown")).isEqualTo(AiCallScope.QA);
    }

    @Test
    @DisplayName("fromUri null 返回 QA")
    void fromUri_null() {
        assertThat(AiCallScope.fromUri(null)).isEqualTo(AiCallScope.QA);
    }

    @Test
    @DisplayName("cacheKeyPrefix 格式正确")
    void cacheKeyPrefix() {
        assertThat(AiCallScope.REVIEW.cacheKeyPrefix())
                .isEqualTo("ai:llm:v1:review:");
    }

    @Test
    @DisplayName("rateLimitKeyPrefix 格式正确")
    void rateLimitKeyPrefix() {
        assertThat(AiCallScope.PRICING.rateLimitKeyPrefix())
                .isEqualTo("ai:rl:pricing:");
    }

    @Test
    @DisplayName("TTL 配置正确")
    void ttlConfig() {
        assertThat(AiCallScope.REVIEW.getTtlSeconds()).isEqualTo(3600);
        assertThat(AiCallScope.QA.getTtlSeconds()).isEqualTo(900);
    }

    @Test
    @DisplayName("限流配置正确")
    void rateLimitConfig() {
        assertThat(AiCallScope.REVIEW.getRatePerMinute()).isEqualTo(10);
        assertThat(AiCallScope.AUTO_LISTING.getRatePerMinute()).isEqualTo(5);
    }
}
