package com.cartethyia.easyorange.framework.web.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.framework.config.properties.IdempotencyProperties;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * RedisIdempotencyService 单元测试 — 幂等 key 的「抢锁 + 缓存结果」语义。
 * <p>
 * 用 {@link ConcurrentHashMap} 模拟 Redis 的 GET / SETNX / SET / DELETE，
 * 重点验证并发重复请求下业务操作只执行一次。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RedisIdempotencyService 幂等")
class RedisIdempotencyServiceTest {

    private static final String KEY = "order-1";

    private final Map<Object, Object> store = new ConcurrentHashMap<>();
    private final AtomicInteger execCount = new AtomicInteger();

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    private RedisIdempotencyService service;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ValueOperations<Object, Object> vo = mock(ValueOperations.class);
        when(vo.get(any())).thenAnswer(inv -> store.get(inv.getArgument(0)));
        when(vo.setIfAbsent(any(), any(), anyLong(), any()))
                .thenAnswer(inv -> store.putIfAbsent(inv.getArgument(0), "1") == null);
        doAnswer(inv -> {
                    store.put(inv.getArgument(0), inv.getArgument(1));
                    return null;
                })
                .when(vo)
                .set(any(), any(), anyLong(), any());

        when(redisTemplate.opsForValue()).thenReturn(vo);
        when(redisTemplate.delete(anyString())).thenAnswer(inv -> store.remove(inv.getArgument(0)) != null);

        IdempotencyProperties properties = new IdempotencyProperties();
        service = new RedisIdempotencyService(redisTemplate, properties);
    }

    private IdempotentOperation<String> countingOperation() {
        return () -> {
            execCount.incrementAndGet();
            return "RESULT";
        };
    }

    @Test
    @DisplayName("已缓存结果 → 直接返回，不重复执行业务")
    void execute_whenCached_returnsCachedWithoutExecuting() throws Exception {
        store.put("eo:idempotency:" + KEY, "CACHED");

        String result = service.execute(KEY, 60, countingOperation());

        assertThat(result).isEqualTo("CACHED");
        assertThat(execCount.get()).isZero();
    }

    @Test
    @DisplayName("未缓存 → 执行业务并缓存结果")
    void execute_whenMiss_executesAndCaches() throws Exception {
        String result = service.execute(KEY, 60, countingOperation());

        assertThat(result).isEqualTo("RESULT");
        assertThat(execCount.get()).isEqualTo(1);
        assertThat(store).containsEntry("eo:idempotency:" + KEY, "RESULT");
    }

    @Test
    @DisplayName("业务异常 → 向上抛且不缓存，重试可重新执行")
    void execute_whenOperationThrows_propagatesAndDoesNotCache() {
        assertThatThrownBy(() -> service.execute(KEY, 60, () -> {
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        // 异常不缓存，且锁已释放
        assertThat(store).doesNotContainKey("eo:idempotency:" + KEY);
        assertThat(store).doesNotContainKey("eo:idempotency:" + KEY + ":lock");
    }

    @Test
    @DisplayName("Redis 读异常 → fail-open：降级执行一次，不抛错")
    void execute_whenRedisReadThrows_failsOpenAndExecutesOnce() throws Exception {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("connection refused"));

        String result = service.execute(KEY, 60, countingOperation());

        assertThat(result).isEqualTo("RESULT");
        assertThat(execCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("并发重复请求同一 key → 业务只执行一次，双方拿到相同结果")
    void execute_whenConcurrentDuplicates_operatesExactlyOnce() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> a = pool.submit(() -> {
                start.await();
                return service.execute(KEY, 60, countingOperation());
            });
            Future<String> b = pool.submit(() -> {
                start.await();
                return service.execute(KEY, 60, countingOperation());
            });
            start.countDown();

            assertThat(a.get(5, TimeUnit.SECONDS)).isEqualTo("RESULT");
            assertThat(b.get(5, TimeUnit.SECONDS)).isEqualTo("RESULT");
        } finally {
            pool.shutdownNow();
        }

        assertThat(execCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("重复顺序调用 → 命中缓存，业务不重复执行")
    void execute_whenCalledTwiceSequentially_executesOnce() throws Exception {
        service.execute(KEY, 60, countingOperation());
        service.execute(KEY, 60, countingOperation());

        assertThat(execCount.get()).isEqualTo(1);
    }
}
