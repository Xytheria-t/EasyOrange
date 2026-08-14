package com.cartethyia.easyorange.product.adapter.outbound.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.product.domain.valueobject.ViewCountEntry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisViewCountCacheAdapter 单元测试")
class RedisViewCountCacheAdapterTest {

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private HashOperations<Object, Object, Object> hashOperations;

    private RedisViewCountCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        adapter = new RedisViewCountCacheAdapter(redisTemplate);
    }

    @Test
    @DisplayName("读取待落库计数，正常条目解析返回")
    void findAllPending_parsesValidEntries() {
        when(hashOperations.entries("eo:product:views:pending")).thenReturn(Map.of("1", 5, "2", 3));

        List<ViewCountEntry> entries = adapter.findAllPending();

        assertThat(entries)
                .hasSize(2)
                .containsExactlyInAnyOrder(new ViewCountEntry("1", 5), new ViewCountEntry("2", 3));
        verify(hashOperations, never()).delete(any(), any());
    }

    @Test
    @DisplayName("解析失败的条目被丢弃并清出缓冲")
    void findAllPending_dropsInvalidEntry() {
        when(hashOperations.entries("eo:product:views:pending")).thenReturn(Map.of("1", 5, "bad", "not-a-number"));

        List<ViewCountEntry> entries = adapter.findAllPending();

        assertThat(entries).containsExactly(new ViewCountEntry("1", 5));
        verify(hashOperations).delete("eo:product:views:pending", "bad");
    }
}
