package com.cartethyia.easyorange.payment.application.lock;

import com.cartethyia.easyorange.payment.application.metrics.PaymentMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributedLockWrapperTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;

    private SimpleMeterRegistry meterRegistry;
    private DistributedLockWrapper wrapper;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        wrapper = new DistributedLockWrapper(redissonClient, new PaymentMetricsService(meterRegistry));
    }

    @Test
    @DisplayName("获取锁失败时记录并发冲突指标并抛异常")
    void executeWithLock_lockFailed_recordsConflictAndThrows() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertThatThrownBy(() -> wrapper.executeWithLock("payment:pay:1", () -> { }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("繁忙");

        Counter counter = meterRegistry.counter("payment.concurrent.conflict.total", "type", "concurrency");
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("获取锁成功时执行操作且不记录并发冲突")
    void executeWithLock_lockAcquired_runsOperationWithoutConflict() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        String result = wrapper.executeWithLock("payment:pay:1", () -> "ok");

        assertThat(result).isEqualTo("ok");
        Counter counter = meterRegistry.counter("payment.concurrent.conflict.total", "type", "concurrency");
        assertThat(counter.count()).isZero();
    }
}
