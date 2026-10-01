package com.cartethyia.easyorange.ai.adapter.outbound.budget;

import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * 内存版 Token 预算存储 — 开发模式使用，重启后清空。
 * <p>
 * 预留与记账都走 {@code AtomicReference} 的 CAS 重试循环，同 key 并发不丢增量 —— 但只在单进程内成立。
 * <b>多副本部署必须切 {@code easyorange.ai.budget.store=redis}</b>，否则 N 个实例各记各的，日限被放大 N 倍。
 * key 为 {@code scenario + ":" + LocalDate.now()} 实现每日隔离，不做 TTL 清理（YAGNI，重启即清空）；
 * 通过 {@link com.cartethyia.easyorange.ai.config.AiConfig#tokenBudgetStore()} 注册为 Bean。
 */
@Slf4j
public class InMemoryTokenBudgetStore implements TokenBudgetStorePort {

    private final Map<String, AtomicReference<TokenUsage>> store = new ConcurrentHashMap<>();

    public InMemoryTokenBudgetStore() {
        log.info("TokenBudgetStorePort: 使用内存版存储（开发模式，重启清空）——多副本部署须切 redis，否则日限被放大 N 倍");
    }

    @Override
    @Nullable
    public TokenReservation tryReserve(String scenario, int amount, int dailyLimit) {
        var ref = store.computeIfAbsent(
                todayKey(scenario), k -> new AtomicReference<>(new TokenUsage(0, 0, 0, System.currentTimeMillis())));
        for (; ; ) {
            var current = ref.get();
            if (dailyLimit > 0 && current.total() + amount > dailyLimit) {
                return null;
            }
            var reserved = new TokenUsage(
                    current.inputTokens(),
                    current.outputTokens(),
                    current.reservedTokens() + amount,
                    System.currentTimeMillis());
            if (ref.compareAndSet(current, reserved)) {
                return () -> ref.updateAndGet(u -> new TokenUsage(
                        u.inputTokens(), u.outputTokens(), Math.max(0, u.reservedTokens() - amount), u.timestamp()));
            }
        }
    }

    @Override
    public Optional<TokenUsage> getTodayUsage(String scenario) {
        var ref = store.get(todayKey(scenario));
        return ref != null ? Optional.of(ref.get()) : Optional.empty();
    }

    @Override
    public void recordUsage(String scenario, int inputTokens, int outputTokens) {
        var key = todayKey(scenario);
        store.computeIfAbsent(key, k -> new AtomicReference<>(new TokenUsage(0, 0, 0, System.currentTimeMillis())))
                .updateAndGet(current -> new TokenUsage(
                        current.inputTokens() + inputTokens,
                        current.outputTokens() + outputTokens,
                        current.reservedTokens(),
                        System.currentTimeMillis()));
    }

    private String todayKey(String scenario) {
        return scenario + ":" + LocalDate.now();
    }
}
