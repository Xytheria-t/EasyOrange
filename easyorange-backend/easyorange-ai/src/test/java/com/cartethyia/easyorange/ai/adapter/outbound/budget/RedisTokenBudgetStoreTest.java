package com.cartethyia.easyorange.ai.adapter.outbound.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.LocalDate;
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
    @DisplayName("记账 -> HINCRBY 累加两个计数并刷新 TTL")
    void recordUsage_incrementsAndExpires() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForHash()).thenReturn(hashOps);

        store.recordUsage("chat", 1200, 300);

        verify(hashOps).increment(CHAT_KEY, RedisTokenBudgetStore.FIELD_INPUT, 1200L);
        verify(hashOps).increment(CHAT_KEY, RedisTokenBudgetStore.FIELD_OUTPUT, 300L);
        verify(redis).expire(CHAT_KEY, Duration.ofHours(48));
    }

    @Test
    @DisplayName("单侧为 0 -> 只写非零计数（embedding 无生成用量时不落多余的 0 增量）")
    void recordUsage_zeroSide_writesOnlyNonZero() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForHash()).thenReturn(hashOps);

        store.recordUsage("knowledge", 500, 0);

        String knowledgeKey = RedisTokenBudgetStore.KEY_PREFIX + "knowledge:" + LocalDate.now();
        verify(hashOps).increment(knowledgeKey, RedisTokenBudgetStore.FIELD_INPUT, 500L);
        verify(hashOps, never()).increment(anyString(), eq(RedisTokenBudgetStore.FIELD_OUTPUT), anyLong());
    }

    @Test
    @DisplayName("Redis 未装配 -> 读空、写不抛（不影响无 Redis 环境启动与对话）")
    void redisUnavailable_failsOpen() {
        when(redisProvider.getIfAvailable()).thenReturn(null);

        assertThat(store.getTodayUsage("chat")).isEmpty();
        store.recordUsage("chat", 1200, 300);
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
    @DisplayName("Redis 写异常 -> 只告警不抛且计数（本次调用不计入，不打断业务链路）")
    void writeFailure_failsOpen() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForHash()).thenThrow(new RuntimeException("connection refused"));

        store.recordUsage("chat", 1200, 300);

        var counter = meterRegistry
                .find("easyorange.ai.budget.failopen")
                .tags("op", "write")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
