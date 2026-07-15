package com.cartethyia.easyorange.ai.budget;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 内存版 Token 预算存储 — 开发模式使用，重启后清空。
 * <p>
 * 使用 {@link ConcurrentHashMap} + {@link AtomicReference} 保证线程安全，
 * key 为 {@code scenario + ":" + LocalDate.now()} 实现每日隔离。
 * 不做 TTL 清理（YAGNI，重启即清空）。
 * <p>
 * 通过 {@link com.cartethyia.easyorange.ai.config.AiConfig#tokenBudgetStore()} 注册为 Bean。
 */
@Slf4j
public class InMemoryTokenBudgetStore implements TokenBudgetStore {

    private final Map<String, AtomicReference<TokenUsage>> store = new ConcurrentHashMap<>();

    public InMemoryTokenBudgetStore() {
        log.info("TokenBudgetStore: 使用内存版存储（开发模式，重启清空）");
    }

    @Override
    public Optional<TokenUsage> getTodayUsage(String scenario) {
        var ref = store.get(todayKey(scenario));
        return ref != null ? Optional.of(ref.get()) : Optional.empty();
    }

    @Override
    public void recordUsage(String scenario, int inputTokens, int outputTokens) {
        var key = todayKey(scenario);
        store.computeIfAbsent(key, k -> new AtomicReference<>(new TokenUsage(0, 0, System.currentTimeMillis())))
                .updateAndGet(current -> new TokenUsage(
                        current.inputTokens() + inputTokens,
                        current.outputTokens() + outputTokens,
                        System.currentTimeMillis()
                ));
    }

    private String todayKey(String scenario) {
        return scenario + ":" + LocalDate.now();
    }
}
