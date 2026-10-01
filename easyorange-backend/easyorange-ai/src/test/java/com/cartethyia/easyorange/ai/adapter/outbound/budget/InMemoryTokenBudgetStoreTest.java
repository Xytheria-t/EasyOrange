package com.cartethyia.easyorange.ai.adapter.outbound.budget;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Test
    @DisplayName("预留越过日限返回 null 且不落任何占用 — 拒绝必须无副作用")
    void tryReserve_overLimit_returnsNullWithoutSideEffect() {
        store.recordUsage("chat", 900, 0);

        assertThat(store.tryReserve("chat", 200, 1000)).isNull();
        assertThat(store.getTodayUsage("chat").orElseThrow().total()).isEqualTo(900);
    }

    @Test
    @DisplayName("释放后只留真实记账 — 预留字段归零、总量回落到真实值")
    void release_reservationRemoved_onlyRealUsageRemains() {
        var reservation = store.tryReserve("chat", 500, 1000);
        assertThat(reservation).isNotNull();
        store.recordUsage("chat", 300, 0);

        reservation.release();

        var usage = store.getTodayUsage("chat").orElseThrow();
        assertThat(usage.reservedTokens()).isZero();
        assertThat(usage.total()).isEqualTo(300);
    }

    @Test
    @DisplayName("日限 0 = 不限：预留永远成功")
    void tryReserve_zeroLimit_alwaysGrants() {
        assertThat(store.tryReserve("chat", 10_000, 0)).isNotNull();
    }

    @Test
    @DisplayName("并发预留不越限 — CAS 循环串行化判与记，成功预留总数受日限约束")
    void tryReserve_concurrentNeverExceedsLimit() throws Exception {
        int threads = 16;
        int perCall = 100;
        int dailyLimit = 1000;
        var latch = new CountDownLatch(threads);
        var granted = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    if (store.tryReserve("chat", perCall, dailyLimit) != null) {
                        granted.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        assertThat(granted.get()).isEqualTo(dailyLimit / perCall);
        assertThat(store.getTodayUsage("chat").orElseThrow().reservedTokens())
                .isEqualTo((long) granted.get() * perCall);
    }
}
