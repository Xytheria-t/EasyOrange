package com.cartethyia.easyorange.product.adapter.outbound.scheduler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.product.application.service.ViewCountBatchProcessor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("ViewCountFlushScheduler 单元测试")
class ViewCountFlushSchedulerTest {

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private ViewCountBatchProcessor batchProcessor;

    @Mock
    private ValueOperations<Object, Object> valueOperations;

    @InjectMocks
    private ViewCountFlushScheduler scheduler;

    @Nested
    @DisplayName("flush")
    class FlushTests {

        @Test
        @DisplayName("获取锁成功后执行批处理")
        void flush_lockAcquired_callsProcessor() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(eq("eo:product:views:lock"), eq("1"), eq(10L), eq(TimeUnit.SECONDS)))
                    .thenReturn(true);

            scheduler.flush();

            verify(batchProcessor).flush();
            verify(redisTemplate).delete("eo:product:views:lock");
        }

        @Test
        @DisplayName("未获取到锁时跳过")
        void flush_lockNotAcquired_skips() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any()))
                    .thenReturn(false);

            scheduler.flush();

            verify(batchProcessor, never()).flush();
            verify(redisTemplate, never()).delete("eo:product:views:lock");
        }

        @Test
        @DisplayName("批处理异常时释放锁")
        void flush_processorError_releasesLock() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any()))
                    .thenReturn(true);
            doThrow(new RuntimeException("Flush error")).when(batchProcessor).flush();

            scheduler.flush();

            verify(redisTemplate).delete("eo:product:views:lock");
        }
    }
}
