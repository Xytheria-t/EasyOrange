package com.cartethyia.easyorange.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.product.application.port.cache.ViewCountPort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ViewCountEntry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("浏览量服务单元测试")
class ProductViewCountServiceTest {

    @Mock
    private ViewCountPort viewCountPort;

    @Mock
    private ProductRepository productRepository;

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
            viewCountService.incrementViewCount("1");

            verify(viewCountPort).increment("1");
        }

        @Test
        @DisplayName("productId为null时跳过")
        void incrementViewCount_nullProductId_noop() {
            viewCountService.incrementViewCount(null);

            verify(viewCountPort, never()).increment(any());
        }

        @Test
        @DisplayName("Redis异常时记录警告不抛出")
        void incrementViewCount_redisError_doesNotThrow() {
            doThrow(new RuntimeException("Redis error")).when(viewCountPort).increment("1");

            viewCountService.incrementViewCount("1");
        }
    }

    @Nested
    @DisplayName("batchProcessor.flush")
    class FlushTests {

        @Test
        @DisplayName("批量同步浏览量成功")
        void flush_success() {
            when(viewCountPort.findAllPending())
                    .thenReturn(List.of(new ViewCountEntry("1", 5), new ViewCountEntry("2", 3)));

            batchProcessor.flush();

            var captor = ArgumentCaptor.forClass(List.class);
            verify(productRepository).batchAddViewCounts(captor.capture());
            assertThat(captor.getValue())
                    .hasSize(2)
                    .containsExactlyInAnyOrder(new ViewCountEntry("1", 5), new ViewCountEntry("2", 3));
            verify(viewCountPort).removePending(List.of("1", "2"));
        }

        @Test
        @DisplayName("待同步列表为空时跳过")
        void flush_empty_skips() {
            when(viewCountPort.findAllPending()).thenReturn(List.of());

            batchProcessor.flush();

            verify(productRepository, never()).batchAddViewCounts(any());
            verify(viewCountPort, never()).removePending(any());
        }

        @Test
        @DisplayName("无有效数据时跳过DB写入")
        void flush_allInvalid_skipsDbWrite() {
            when(viewCountPort.findAllPending()).thenReturn(List.of());

            batchProcessor.flush();

            verify(productRepository, never()).batchAddViewCounts(any());
            verify(viewCountPort, never()).removePending(any());
        }

        @Test
        @DisplayName("Redis读取异常时抛出")
        void flush_redisError_propagates() {
            when(viewCountPort.findAllPending()).thenThrow(new RuntimeException("Redis error"));

            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> batchProcessor.flush());

            verify(productRepository, never()).batchAddViewCounts(any());
        }
    }
}
