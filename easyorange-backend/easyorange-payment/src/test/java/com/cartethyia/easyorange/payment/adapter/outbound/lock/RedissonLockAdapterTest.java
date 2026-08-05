package com.cartethyia.easyorange.payment.adapter.outbound.lock;

import com.cartethyia.easyorange.payment.domain.exception.LockAcquisitionException;
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
@DisplayName("RedissonLockAdapter 锁行为测试")
class RedissonLockAdapterTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;

    private RedissonLockAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedissonLockAdapter(redissonClient);
    }

    @Test
    @DisplayName("获取锁失败时抛出 LockAcquisitionException")
    void executeWithLock_lockFailed_throws() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertThatThrownBy(() -> adapter.executeWithLock("payment:pay:1", () -> { }))
                .isInstanceOf(LockAcquisitionException.class)
                .hasMessageContaining("繁忙");
    }

    @Test
    @DisplayName("获取锁成功时执行操作并返回结果")
    void executeWithLock_lockAcquired_runsOperation() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        String result = adapter.executeWithLock("payment:pay:1", () -> "ok");

        assertThat(result).isEqualTo("ok");
    }
}