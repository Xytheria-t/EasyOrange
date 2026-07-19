package com.cartethyia.easyorange.product.application.service;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductViewCountAppService 单元测试")
class ProductViewCountServiceTest {

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private HashOperations<Object, Object, Object> hashOperations;

    @Mock
    private ValueOperations<Object, Object> valueOperations;

    @Captor
    private ArgumentCaptor<Set<Object>> deleteKeysCaptor;

    @InjectMocks
    private ProductViewCountAppService viewCountService;

    @Nested
    @DisplayName("incrementViewCount")
    class IncrementViewCountTests {

        @Test
        @DisplayName("增加浏览量成功")
        void incrementViewCount_success() {
            when(redisTemplate.opsForHash()).thenReturn(hashOperations);

            viewCountService.incrementViewCount("1");

            verify(hashOperations).increment("eo:product:views:pending", "1", 1);
        }

        @Test
        @DisplayName("productId为null时跳过")
        void incrementViewCount_nullProductId_noop() {
            viewCountService.incrementViewCount(null);

            verify(redisTemplate, never()).opsForHash();
        }

        @Test
        @DisplayName("Redis异常时记录警告不抛出")
        void incrementViewCount_redisError_doesNotThrow() {
            when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("Redis error"));

            viewCountService.incrementViewCount("1");
        }
    }

    @Nested
    @DisplayName("flushViewCountBatch")
    class FlushViewCountBatchTests {

        @Test
        @DisplayName("批量同步浏览量成功")
        void flushViewCountBatch_success() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(eq("eo:product:views:lock"), eq("1"), eq(10L), eq(TimeUnit.SECONDS)))
                    .thenReturn(true);
            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            when(hashOperations.entries("eo:product:views:pending"))
                    .thenReturn(Map.of("1", "5", "2", "3"));

            viewCountService.flushViewCountBatch();

            verify(productMapper).batchAddViewCounts(Map.of("1", 5, "2", 3));
            verify(hashOperations).delete(eq("eo:product:views:pending"), any(Object[].class));
            verify(redisTemplate).delete("eo:product:views:lock");
        }

        @Test
        @DisplayName("未获取到锁时跳过")
        void flushViewCountBatch_lockNotAcquired_skips() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any()))
                    .thenReturn(false);

            viewCountService.flushViewCountBatch();

            verify(productMapper, never()).batchAddViewCounts(any());
            verify(redisTemplate, never()).delete("eo:product:views:lock");
        }

        @Test
        @DisplayName("待同步列表为空时跳过")
        void flushViewCountBatch_empty_skips() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any()))
                    .thenReturn(true);
            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            when(hashOperations.entries("eo:product:views:pending"))
                    .thenReturn(Map.of());

            viewCountService.flushViewCountBatch();

            verify(productMapper, never()).batchAddViewCounts(any());
            verify(redisTemplate).delete("eo:product:views:lock");
        }

        @Test
        @DisplayName("解析无效的数据时跳过")
        void flushViewCountBatch_invalidData_skips() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any()))
                    .thenReturn(true);
            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            when(hashOperations.entries("eo:product:views:pending"))
                    .thenReturn(Map.of("100", "invalid"));

            viewCountService.flushViewCountBatch();

            verify(productMapper, never()).batchAddViewCounts(any());
            verify(redisTemplate).delete("eo:product:views:lock");
        }

        @Test
        @DisplayName("异常时释放锁")
        void flushViewCountBatch_error_releasesLock() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any()))
                    .thenReturn(true);
            when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("Redis error"));

            viewCountService.flushViewCountBatch();

            verify(redisTemplate).delete("eo:product:views:lock");
        }
    }
}
