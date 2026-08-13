package com.cartethyia.easyorange.framework.event.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * EventIdempotencyChecker 单元测试 — 「SET NX EX 原子领取 + 失败撤销」语义。
 * <p>
 * 用 {@link ConcurrentHashMap} 模拟 Redis 的 SETNX / DELETE，验证并发场景下只有一个消费者能拿到处理权。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EventIdempotencyChecker 事件幂等")
class EventIdempotencyCheckerTest {

    private static final String EVENT_TYPE = "OrderNotificationEventConsumer:OrderCreated";
    private static final String EVENT_ID = "0196a1c2-0000-7000-8000-000000000001";
    private static final String DONE_KEY = "eo:event:done:" + EVENT_TYPE + ":" + EVENT_ID;

    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Mock
    private StringRedisTemplate redisTemplate;

    private EventIdempotencyChecker checker;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenAnswer(inv -> store.putIfAbsent(inv.getArgument(0), inv.getArgument(1)) == null);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.delete(anyString())).thenAnswer(inv -> store.remove(inv.getArgument(0)) != null);
        checker = new EventIdempotencyChecker(redisTemplate);
    }

    @Test
    @DisplayName("首次领取返回 true 并写入 24h 去重标记")
    void tryMark_firstClaim_returnsTrueAndWritesMark() {
        assertThat(checker.tryMark(EVENT_TYPE, EVENT_ID)).isTrue();
        assertThat(store).containsKey(DONE_KEY);
    }

    @Test
    @DisplayName("重复领取返回 false（标记已存在）")
    void tryMark_duplicateClaim_returnsFalse() {
        assertThat(checker.tryMark(EVENT_TYPE, EVENT_ID)).isTrue();
        assertThat(checker.tryMark(EVENT_TYPE, EVENT_ID)).isFalse();
        assertThat(store).hasSize(1);
    }

    @Test
    @DisplayName("不同事件互不影响")
    void tryMark_differentEvent_returnsTrue() {
        assertThat(checker.tryMark(EVENT_TYPE, EVENT_ID)).isTrue();
        assertThat(checker.tryMark(EVENT_TYPE, EVENT_ID + "-2")).isTrue();
        assertThat(store).hasSize(2);
    }

    @Test
    @DisplayName("写入使用 SET NX EX：key / 标记值 / 24h TTL")
    void tryMark_usesAtomicSetNxEx() {
        checker.tryMark(EVENT_TYPE, EVENT_ID);

        verify(redisTemplate.opsForValue()).setIfAbsent(eq(DONE_KEY), eq("1"), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("unmark 撤销标记后可重新领取（失败重试路径）")
    void unmark_afterUnmark_canReclaim() {
        assertThat(checker.tryMark(EVENT_TYPE, EVENT_ID)).isTrue();

        checker.unmark(EVENT_TYPE, EVENT_ID);

        assertThat(store).doesNotContainKey(DONE_KEY);
        assertThat(checker.tryMark(EVENT_TYPE, EVENT_ID)).isTrue();
    }

    @Test
    @DisplayName("unmark 删除不存在的标记不抛异常")
    void unmark_missingMark_isNoOp() {
        checker.unmark(EVENT_TYPE, EVENT_ID);

        assertThat(store).isEmpty();
    }
}
