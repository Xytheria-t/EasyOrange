package com.cartethyia.easyorange.framework.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.framework.config.properties.LockProperties;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DistributedRedissonLockAdapter 多锁行为测试")
class DistributedLockAdapterTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock1;

    @Mock
    private RLock lock2;

    private DistributedRedissonLockAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DistributedRedissonLockAdapter(redissonClient, new LockProperties());
        when(lock1.isHeldByCurrentThread()).thenReturn(true);
        when(lock2.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    @DisplayName("全部锁获取成功：执行操作并逆序释放所有锁")
    void executeWithLocks_allAcquired_runsOperationAndReleases() throws InterruptedException {
        when(redissonClient.getLock("eo:order:lock:product:100")).thenReturn(lock1);
        when(redissonClient.getLock("eo:order:lock:product:200")).thenReturn(lock2);
        when(lock1.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock2.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        AtomicBoolean executed = new AtomicBoolean(false);
        String result = adapter.executeWithLocks(
                List.of("eo:order:lock:product:100", "eo:order:lock:product:200"), 10, () -> {
                    executed.set(true);
                    return "ok";
                });

        assertThat(result).isEqualTo("ok");
        assertThat(executed).isTrue();
        verify(lock2).unlock();
        verify(lock1).unlock();
    }

    @Test
    @DisplayName("第二把锁获取失败：回滚第一把锁，操作不执行")
    void executeWithLocks_secondLockFails_releasesFirstAndThrows() throws InterruptedException {
        when(redissonClient.getLock("p1")).thenReturn(lock1);
        when(redissonClient.getLock("p2")).thenReturn(lock2);
        when(lock1.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock2.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        AtomicBoolean executed = new AtomicBoolean(false);
        assertThatThrownBy(() -> adapter.executeWithLocks(List.of("p1", "p2"), 10, () -> {
                    executed.set(true);
                    return null;
                }))
                .isInstanceOf(LockAcquisitionException.class);

        assertThat(executed).isFalse();
        verify(lock1).unlock();
        verify(lock2, never()).unlock();
    }

    @Test
    @DisplayName("获取锁被中断：恢复中断位并抛异常")
    void executeWithLocks_interrupted_reinterruptsAndThrows() throws InterruptedException {
        when(redissonClient.getLock("p1")).thenReturn(lock1);
        when(lock1.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());

        assertThatThrownBy(() -> adapter.executeWithLocks(List.of("p1"), 10, () -> null))
                .isInstanceOf(LockAcquisitionException.class);

        // assert + 清除中断位，避免污染后续测试线程
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    @DisplayName("事务内：锁推迟到 afterCompletion（提交/回滚）后才释放")
    void executeWithLocks_inTransaction_releasesOnlyAfterCompletion() throws InterruptedException {
        when(redissonClient.getLock("p1")).thenReturn(lock1);
        when(lock1.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        TransactionSynchronizationManager.initSynchronization();
        try {
            adapter.executeWithLocks(List.of("p1"), 10, () -> null);

            // 操作已完成，但事务尚未提交时锁不得释放
            verify(lock1, never()).unlock();

            // 模拟事务提交：触发 afterCompletion 后锁才释放
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
            verify(lock1).unlock();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("waitTimeout=0 即非阻塞尝试：拿不到立即失败，可被调用方捕获降级")
    void executeWithLocks_zeroWaitTimeout_nonBlockingTry() throws InterruptedException {
        when(redissonClient.getLock("p1")).thenReturn(lock1);
        when(lock1.tryLock(eq(0L), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertThatThrownBy(() -> adapter.executeWithLocks(List.of("p1"), 0, () -> "should-not-run"))
                .isInstanceOf(LockAcquisitionException.class);
        verify(lock1, never()).unlock();
    }
}