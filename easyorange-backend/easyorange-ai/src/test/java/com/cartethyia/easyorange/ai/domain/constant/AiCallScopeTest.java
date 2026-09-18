package com.cartethyia.easyorange.ai.domain.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiCallScope 测试")
class AiCallScopeTest {

    @Test
    @DisplayName("fromUri 匹配 pricing")
    void fromUri_pricing() {
        assertThat(AiCallScope.fromUri("/api/ai/pricing")).isEqualTo(AiCallScope.PRICING);
    }

    @Test
    @DisplayName("fromUri 匹配 review（审核建议只剩管理端入口）")
    void fromUri_review() {
        // /api/ai/review 已于 2026-09-18 删除（无调用方的重复入口）；REVIEW 场景现由管理端触发
        assertThat(AiCallScope.fromUri("/api/admin/products/p-1/ai-review")).isEqualTo(AiCallScope.REVIEW);
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
        assertThat(AiCallScope.REVIEW.cacheKeyPrefix()).isEqualTo("ai:stale:review:");
    }

    @Test
    @DisplayName("rateLimitKeyPrefix 格式正确")
    void rateLimitKeyPrefix() {
        assertThat(AiCallScope.PRICING.rateLimitKeyPrefix()).isEqualTo("ai:rl:pricing:");
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
        assertThat(AiCallScope.REVIEW.getRatePerMinute()).isEqualTo(10);
        assertThat(AiCallScope.AUTO_LISTING.getRatePerMinute()).isEqualTo(5);
    }
}
