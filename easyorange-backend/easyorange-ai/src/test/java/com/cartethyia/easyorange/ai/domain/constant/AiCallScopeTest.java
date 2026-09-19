package com.cartethyia.easyorange.ai.domain.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiCallScope 测试")
class AiCallScopeTest {

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
    @DisplayName("fromUri 通过 products/search 片段匹配到语义召回场景")
    void fromUri_semantic() {
        assertThat(AiCallScope.fromUri("/api/products/search")).isEqualTo(AiCallScope.SEMANTIC);
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
        assertThat(AiCallScope.QA.cacheKeyPrefix()).isEqualTo("ai:stale:qa:");
    }

    @Test
    @DisplayName("rateLimitKeyPrefix 格式正确")
    void rateLimitKeyPrefix() {
        assertThat(AiCallScope.AUTO_LISTING.rateLimitKeyPrefix()).isEqualTo("ai:rl:auto_listing:");
    }

    @Test
    @DisplayName("budgetScenario 与限流前缀同源（场景名必须一致，否则预算记账与检查读两处）")
    void budgetScenarioMatchesRateLimitPrefix() {
        for (AiCallScope scope : AiCallScope.values()) {
            assertThat(scope.rateLimitKeyPrefix()).isEqualTo("ai:rl:" + scope.budgetScenario() + ":");
        }
    }

    @Test
    @DisplayName("限流配置正确")
    void rateLimitConfig() {
        assertThat(AiCallScope.QA.getRatePerMinute()).isEqualTo(20);
        assertThat(AiCallScope.AUTO_LISTING.getRatePerMinute()).isEqualTo(5);
    }
}
