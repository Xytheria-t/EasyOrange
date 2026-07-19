package com.cartethyia.easyorange.product.application.service;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.SearchHistoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.SearchHistoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchHistoryBufferAppService 单元测试")
class SearchHistoryBufferServiceTest {

    @Mock
    private SearchHistoryMapper searchHistoryMapper;

    @InjectMocks
    private SearchHistoryBufferAppService bufferService;

    @Captor
    private ArgumentCaptor<List<SearchHistoryDO>> batchCaptor;

    @Nested
    @DisplayName("addToBuffer")
    class AddToBufferTests {

        @Test
        @DisplayName("添加搜索历史到缓冲区成功")
        void addToBuffer_success() {
            bufferService.addToBuffer("1", "手机");

            assertThat(bufferService.getBufferSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("userId为null时不添加")
        void addToBuffer_nullUserId_noop() {
            bufferService.addToBuffer(null, "手机");

            assertThat(bufferService.getBufferSize()).isEqualTo(0);
        }

        @Test
        @DisplayName("keyword为null时不添加")
        void addToBuffer_nullKeyword_noop() {
            bufferService.addToBuffer("1", null);

            assertThat(bufferService.getBufferSize()).isEqualTo(0);
        }

        @Test
        @DisplayName("keyword为空白时不添加")
        void addToBuffer_blankKeyword_noop() {
            bufferService.addToBuffer("1", "   ");

            assertThat(bufferService.getBufferSize()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("flushBuffer")
    class FlushBufferTests {

        @Test
        @DisplayName("缓冲区为空时跳过刷入")
        void flushBuffer_empty_noop() {
            bufferService.flushBuffer();

            verify(searchHistoryMapper, never()).batchInsert(any());
        }

        @Test
        @DisplayName("刷入缓冲区成功")
        void flushBuffer_success() {
            bufferService.addToBuffer("1", "手机");
            bufferService.addToBuffer("1", "电脑");

            bufferService.flushBuffer();

            verify(searchHistoryMapper).batchInsert(batchCaptor.capture());
            List<SearchHistoryDO> batch = batchCaptor.getValue();
            assertThat(batch).hasSize(2);
            assertThat(bufferService.getBufferSize()).isEqualTo(0);
        }

        @Test
        @DisplayName("批量插入失败后回退到缓冲区")
        void flushBuffer_failure_requeue() {
            bufferService.addToBuffer("1", "手机");
            doThrow(new RuntimeException("DB error")).when(searchHistoryMapper).batchInsert(any());

            bufferService.flushBuffer();

            verify(searchHistoryMapper).batchInsert(any());
            assertThat(bufferService.getBufferSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("超过批量大小时仅刷入BATCH_SIZE条")
        void flushBuffer_respectsBatchSize() {
            for (int i = 0; i < 150; i++) {
                bufferService.addToBuffer("1", "keyword" + i);
            }

            bufferService.flushBuffer();

            verify(searchHistoryMapper).batchInsert(batchCaptor.capture());
            assertThat(batchCaptor.getValue()).hasSize(100);
            assertThat(bufferService.getBufferSize()).isEqualTo(50);
        }
    }
}
