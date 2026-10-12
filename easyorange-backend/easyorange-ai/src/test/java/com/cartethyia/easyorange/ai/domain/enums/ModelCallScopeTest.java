package com.cartethyia.easyorange.ai.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ModelCallScope 测试")
class ModelCallScopeTest {

    @Test
    @DisplayName("fromUri 匹配 auto-listing")
    void fromUri_autoListing() {
        assertThat(ModelCallScope.fromUri("/api/ai/auto-listing")).isEqualTo(ModelCallScope.AUTO_LISTING);
    }

    @Test
    @DisplayName("fromUri 通过 products/search 片段匹配到语义召回场景")
    void fromUri_semantic() {
        assertThat(ModelCallScope.fromUri("/api/products/search")).isEqualTo(ModelCallScope.SEMANTIC);
    }

    @Test
    @DisplayName("fromUri 未匹配返回 CHAT")
    void fromUri_unknown() {
        assertThat(ModelCallScope.fromUri("/api/ai/unknown")).isEqualTo(ModelCallScope.CHAT);
    }

    @Test
    @DisplayName("fromUri 子串不得命中 — 片段出现在别的路径段里不算（防未来 URI 意外落入已有场景）")
    void fromUri_substringNoMatch() {
        assertThat(ModelCallScope.fromUri("/api/ai/chatty")).isEqualTo(ModelCallScope.CHAT);
        assertThat(ModelCallScope.fromUri("/api/ai/products/search/history")).isEqualTo(ModelCallScope.CHAT);
        assertThat(ModelCallScope.fromUri("/api/ai/auto-listing-x")).isEqualTo(ModelCallScope.CHAT);
    }

    @Test
    @DisplayName("fromUri 匹配场景下的子路径（chat/stream、knowledge/search 也要落到各自场景）")
    void fromUri_subPath() {
        assertThat(ModelCallScope.fromUri("/api/ai/chat/stream")).isEqualTo(ModelCallScope.CHAT);
        assertThat(ModelCallScope.fromUri("/api/ai/knowledge/search")).isEqualTo(ModelCallScope.KNOWLEDGE);
    }

    @Test
    @DisplayName("fromUri null 返回 CHAT")
    void fromUri_null() {
        assertThat(ModelCallScope.fromUri(null)).isEqualTo(ModelCallScope.CHAT);
    }

    @Test
    @DisplayName("cacheKeyPrefix 格式正确")
    void cacheKeyPrefix() {
        assertThat(ModelCallScope.CHAT.cacheKeyPrefix()).isEqualTo("ai:stale:chat:");
    }

    @Test
    @DisplayName("rateLimitKeyPrefix 格式正确")
    void rateLimitKeyPrefix() {
        assertThat(ModelCallScope.AUTO_LISTING.rateLimitKeyPrefix()).isEqualTo("ai:rl:auto_listing:");
    }

    @Test
    @DisplayName("budgetScenario 与限流前缀同源（场景名必须一致，否则预算记账与检查读两处）")
    void budgetScenarioMatchesRateLimitPrefix() {
        for (ModelCallScope scope : ModelCallScope.values()) {
            assertThat(scope.rateLimitKeyPrefix()).isEqualTo("ai:rl:" + scope.budgetScenario() + ":");
        }
    }

    @Test
    @DisplayName("限流配置正确")
    void rateLimitConfig() {
        assertThat(ModelCallScope.CHAT.getRatePerMinute()).isEqualTo(20);
        assertThat(ModelCallScope.AUTO_LISTING.getRatePerMinute()).isEqualTo(5);
    }
}
