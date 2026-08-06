package com.cartethyia.easyorange.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("浏览量服务单元测试")
class ProductViewCountServiceTest {

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private HashOperations<Object, Object, Object> hashOperations;

    @Mock
    private ValueOperations<Object, Object> valueOperations;

    @InjectMocks
    private ProductViewCountAppService viewCountService;

    @InjectMocks
    private ViewCountBatchProcessor batchProcessor;

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
    @DisplayName("batchProcessor.flush")
    class FlushTests {

        @Test
        @DisplayName("批量同步浏览量成功")
        @SuppressWarnings("unchecked")
        void flush_success() {
            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            when(hashOperations.entries("eo:product:views:pending")).thenReturn(Map.of("1", 5, "2", 3));

            batchProcessor.flush();

            var captor = ArgumentCaptor.forClass(List.class);
            verify(productMapper).batchAddViewCounts(captor.capture());
            assertThat(captor.getValue())
                    .hasSize(2)
                    .containsExactlyInAnyOrder(
                            new ProductMapper.ViewCountEntry("1", 5), new ProductMapper.ViewCountEntry("2", 3));
            verify(hashOperations).delete(eq("eo:product:views:pending"), any(Object[].class));
        }

        @Test
        @DisplayName("待同步列表为空时跳过")
        void flush_empty_skips() {
            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            when(hashOperations.entries("eo:product:views:pending")).thenReturn(Map.of());

            batchProcessor.flush();

            verify(productMapper, never()).batchAddViewCounts(any());
        }

        @Test
        @DisplayName("所有数据解析失败时跳过DB写入，数据保留在Redis中等待下次重试")
        void flush_allInvalid_skipsDbWrite() {
            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            when(hashOperations.entries("eo:product:views:pending")).thenReturn(Map.of("100", "not-a-number"));

            batchProcessor.flush();

            verify(productMapper, never()).batchAddViewCounts(any());
            verify(hashOperations, never()).delete(any(), any());
        }

        @Test
        @DisplayName("Redis读取异常时抛出")
        void flush_redisError_propagates() {
            when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("Redis error"));

            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> batchProcessor.flush());

            verify(productMapper, never()).batchAddViewCounts(any());
        }
    }
}
