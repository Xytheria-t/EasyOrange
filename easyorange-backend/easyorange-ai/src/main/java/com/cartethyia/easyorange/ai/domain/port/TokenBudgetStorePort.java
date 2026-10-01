package com.cartethyia.easyorange.ai.domain.port;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Token 预算存储接口 — 按场景隔离的每日 token 用量，读侧检查 + 原子预留两段协议。
 * <p>
 * 预留（{@link #tryReserve}）是并发正确性的收口：读用量、判限额、记占用必须在一次原子操作里完成
 * （Redis 单脚本 Lua / 内存 CAS），否则 N 个并发请求各自拿着同一份旧用量通过检查，日限被突破
 * 「并发数 × maxPerCall」。真实用量仍由记账方（AiCallRecorder）按供应商回报另行累加。
 */
public interface TokenBudgetStorePort {

    /**
     * 用量快照 — {@code total()} 是预算判定口径：真实记账 + 在途预留（并发请求的估算占用），不只是已记账值。
     */
    record TokenUsage(int inputTokens, int outputTokens, int reservedTokens, long timestamp) {
        public int total() {
            return inputTokens + outputTokens + reservedTokens;
        }
    }

    /** 一次成功预留的释放句柄 — 请求结束（含异常路径）必须调用；释放后预留字段归零，只留真实记账。 */
    interface TokenReservation {
        void release();

        /** fail-open 空句柄 — 存储故障放行时返回，release 无副作用。 */
        TokenReservation NOOP = () -> {};
    }

    /**
     * 原子预留 {@code amount}：占用后越过 {@code dailyTokenLimit} 返回 {@code null}（目标调用不应发起），
     * 成功返回释放句柄。{@code dailyTokenLimit <= 0} 表示不限。实现方故障时 fail-open：放行并返回空句柄。
     */
    @Nullable
    TokenReservation tryReserve(String scenario, int amount, int dailyLimit);

    /** 指定场景今日的累计用量（含在途预留），无记录返回 {@link Optional#empty()}。 */
    Optional<TokenUsage> getTodayUsage(String scenario);

    /** 记录指定场景的 token 用量（累加到今日统计）。 */
    void recordUsage(String scenario, int inputTokens, int outputTokens);
}
