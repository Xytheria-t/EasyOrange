package com.cartethyia.easyorange.ai.adapter.outbound.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisTokenBudgetStore (跨实例日预算) -> 测试")
class RedisTokenBudgetStoreTest {

    private static final String CHAT_KEY = RedisTokenBudgetStore.KEY_PREFIX + "chat:" + LocalDate.now();

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ObjectProvider<StringRedisTemplate> redisProvider;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    private RedisTokenBudgetStore store;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        store = new RedisTokenBudgetStore(redisProvider, meterRegistry);
    }

    @Test
    @DisplayName("无记录 -> empty（与内存版同语义，预算判定按今日未用量）")
    void getTodayUsage_noRecord_returnsEmpty() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries(CHAT_KEY)).thenReturn(Map.of());

        assertThat(store.getTodayUsage("chat")).isEmpty();
    }

    @Test
    @DisplayName("有计数 -> 解析出输入 / 输出与合计（key 按 场景 + 日期 隔离）")
    void getTodayUsage_parsesCounters() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries(CHAT_KEY))
                .thenReturn(
                        Map.of(RedisTokenBudgetStore.FIELD_INPUT, "1200", RedisTokenBudgetStore.FIELD_OUTPUT, "300"));

        var usage = store.getTodayUsage("chat");

        assertThat(usage).isPresent();
        assertThat(usage.get().inputTokens()).isEqualTo(1200);
        assertThat(usage.get().outputTokens()).isEqualTo(300);
        assertThat(usage.get().total()).isEqualTo(1500);
    }

    @Test
    @DisplayName("记账 -> 单脚本携带两个非零增量与 TTL 执行（HINCRBY 与 EXPIRE 同窗口，不再有半笔账）")
    void recordUsage_executesAtomicScript() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);

        store.recordUsage("chat", 1200, 300);

        verify(redis)
                .execute(
                        any(RedisScript.class),
                        eq(List.of(CHAT_KEY)),
                        eq("1200"),
                        eq("300"),
                        eq(String.valueOf(java.time.Duration.ofHours(48).toSeconds())));
    }

    @Test
    @DisplayName("预留成功 -> 返回释放句柄；句柄释放走同一 key 的负增量脚本")
    void tryReserve_granted_returnsReleasableHandle() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1500L);

        var reservation = store.tryReserve("chat", 1500, 300_000);

        assertThat(reservation).isNotNull();
        reservation.release();
        // 释放与预留同一个 key：句柄闭包携带场景与预留量，不依赖调用方记得参数
        verify(redis, org.mockito.Mockito.times(2))
                .execute(any(RedisScript.class), eq(List.of(CHAT_KEY)), any(Object[].class));
    }

    @Test
    @DisplayName("预留越限（脚本返回 -1）-> null，目标调用不应发起")
    void tryReserve_denied_returnsNull() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(-1L);

        assertThat(store.tryReserve("chat", 1500, 300_000)).isNull();
    }

    @Test
    @DisplayName("Redis 未装配 -> 读空、预留放行空句柄、记账不抛（不影响无 Redis 环境启动与对话）")
    void redisUnavailable_failsOpen() {
        when(redisProvider.getIfAvailable()).thenReturn(null);

        assertThat(store.getTodayUsage("chat")).isEmpty();
        var reservation = store.tryReserve("chat", 1500, 300_000);
        assertThat(reservation).isNotNull();
        reservation.release();
        store.recordUsage("chat", 1200, 300);
        verify(redis, never()).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    @Test
    @DisplayName("脚本执行异常 -> 预留 fail-open 放行空句柄且计数（放行的唯一统计面）")
    void reserveFailure_failsOpenWithCounter() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RuntimeException("connection refused"));

        var reservation = store.tryReserve("chat", 1500, 300_000);

        assertThat(reservation).isNotNull();
        var counter = meterRegistry
                .find("easyorange.ai.budget.failopen")
                .tags("op", "reserve")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Redis 读异常 -> fail-open 返回 empty 且计数（吞异常可以、吞统计不行）")
    void readFailure_failsOpen() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForHash()).thenThrow(new RuntimeException("connection refused"));

        assertThat(store.getTodayUsage("chat")).isEmpty();

        var counter = meterRegistry
                .find("easyorange.ai.budget.failopen")
                .tags("op", "read")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("记账脚本异常 -> 只告警不抛且计数（本次调用不计入，不打断业务链路）")
    void writeFailure_failsOpen() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RuntimeException("connection refused"));

        store.recordUsage("chat", 1200, 300);

        var counter = meterRegistry
                .find("easyorange.ai.budget.failopen")
                .tags("op", "write")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
