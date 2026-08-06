package com.cartethyia.easyorange.ai.budget;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryTokenBudgetStore 测试")
class InMemoryTokenBudgetStoreTest {

    private InMemoryTokenBudgetStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryTokenBudgetStore();
    }

    @Test
    @DisplayName("无记录时 getTodayUsage 返回 empty")
    void getTodayUsage_noRecord_returnsEmpty() {
        assertThat(store.getTodayUsage("any_scenario")).isEmpty();
    }

    @Test
    @DisplayName("recordUsage 后 getTodayUsage 返回累计用量")
    void recordUsage_thenGetTodayUsage_returnsAccumulated() {
        store.recordUsage("pricing", 100, 50);

        var usage = store.getTodayUsage("pricing");

        assertThat(usage).isPresent();
        assertThat(usage.get().inputTokens()).isEqualTo(100);
        assertThat(usage.get().outputTokens()).isEqualTo(50);
        assertThat(usage.get().total()).isEqualTo(150);
    }

    @Test
    @DisplayName("多次 recordUsage 累加 token 用量")
    void recordUsage_multipleCalls_accumulates() {
        store.recordUsage("pricing", 100, 50);
        store.recordUsage("pricing", 200, 100);

        var usage = store.getTodayUsage("pricing");

        assertThat(usage).isPresent();
        assertThat(usage.get().inputTokens()).isEqualTo(300);
        assertThat(usage.get().outputTokens()).isEqualTo(150);
        assertThat(usage.get().total()).isEqualTo(450);
    }

    @Test
    @DisplayName("不同场景的用量相互隔离")
    void recordUsage_differentScenarios_isolated() {
        store.recordUsage("pricing", 100, 50);
        store.recordUsage("review", 200, 100);

        var pricingUsage = store.getTodayUsage("pricing");
        var reviewUsage = store.getTodayUsage("review");

        assertThat(pricingUsage).isPresent();
        assertThat(pricingUsage.get().total()).isEqualTo(150);
        assertThat(reviewUsage).isPresent();
        assertThat(reviewUsage.get().total()).isEqualTo(300);
    }

    @Test
    @DisplayName("timestamp 为正值")
    void recordUsage_timestampIsPositive() {
        store.recordUsage("pricing", 100, 50);

        var usage = store.getTodayUsage("pricing");

        assertThat(usage).isPresent();
        assertThat(usage.get().timestamp()).isPositive();
    }
}
