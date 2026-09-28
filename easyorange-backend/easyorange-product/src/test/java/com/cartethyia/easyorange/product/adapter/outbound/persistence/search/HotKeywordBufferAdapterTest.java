package com.cartethyia.easyorange.product.adapter.outbound.persistence.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotKeywordBufferAdapter 单元测试")
class HotKeywordBufferAdapterTest {

    @Mock
    private HotKeywordMapper hotKeywordMapper;

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private ZSetOperations<Object, Object> zSetOperations;

    @InjectMocks
    private HotKeywordBufferAdapter adapter;

    @Captor
    private ArgumentCaptor<List<HotKeywordDO>> batchCaptor;

    @Test
    @DisplayName("同一词搜多次只落一行，search_count 记增量")
    void flush_aggregatesSameKeyword() {
        stubZset();
        adapter.record("手机");
        adapter.record("手机");
        adapter.record(" 相机 ");

        adapter.flush();

        verify(hotKeywordMapper).batchInsertOrUpdate(batchCaptor.capture());
        var batch = batchCaptor.getValue();
        assertThat(batch).hasSize(2);
        assertThat(batch)
                .filteredOn(k -> "手机".equals(k.getKeyword()))
                .singleElement()
                .extracting(HotKeywordDO::getSearchCount)
                .isEqualTo(2);
        // 关键词归一：前后空白不该让同一个词裂成两行
        assertThat(batch).extracting(HotKeywordDO::getKeyword).contains("相机");
        assertThat(adapter.pendingSize()).isZero();
    }

    @Test
    @DisplayName("Redis 不可用时仍把计数留给 DB 兜底，且不抛——热词是非关键路径")
    void record_redisDown_stillBuffers() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        doThrow(new RuntimeException("redis down"))
                .when(zSetOperations)
                .incrementScore(anyString(), any(), anyDouble());

        assertThatCode(() -> adapter.record("手机")).doesNotThrowAnyException();

        assertThat(adapter.pendingSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("落库失败不抛、不回插：热词计数丢一轮只影响榜单排序")
    void flush_dbDown_doesNotThrow() {
        stubZset();
        adapter.record("手机");
        doThrow(new RuntimeException("db down")).when(hotKeywordMapper).batchInsertOrUpdate(any());

        assertThatCode(() -> adapter.flush()).doesNotThrowAnyException();
        assertThat(adapter.pendingSize()).isZero();
    }

    @Test
    @DisplayName("空白关键词不计数")
    void record_blank_noop() {
        adapter.record("   ");

        assertThat(adapter.pendingSize()).isZero();
        adapter.flush();
        verify(hotKeywordMapper, never()).batchInsertOrUpdate(any());
    }

    private void stubZset() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }
}
