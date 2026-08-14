package com.cartethyia.easyorange.ai.adapter.outbound.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.chat.ChatTurn;
import com.cartethyia.easyorange.ai.config.AiProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatSessionStore (Redis 短期记忆) -> 测试")
class ChatSessionStoreTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ObjectProvider<StringRedisTemplate> redisProvider;

    @Mock
    private ListOperations<String, String> listOps;

    private ChatSessionStore store;

    @BeforeEach
    void setUp() {
        when(redis.opsForList()).thenReturn(listOps);
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        store = new ChatSessionStore(redisProvider, new ObjectMapper(), new AiProperties());
    }

    @Test
    @DisplayName("保存轮次 -> 右推 + 裁剪 + 刷新 TTL")
    void saveTurn() {
        store.saveTurn("sess-1", "user", "你好");

        verify(listOps).rightPush("eo:chat:session:sess-1", "{\"role\":\"user\",\"content\":\"你好\"}");
        verify(listOps).trim("eo:chat:session:sess-1", -12L, -1);
        verify(redis).expire("eo:chat:session:sess-1", Duration.ofHours(24));
    }

    @Test
    @DisplayName("读取最近轮次 -> 反序列化为 ChatTurn 列表")
    void loadRecent() {
        when(listOps.range("eo:chat:session:sess-1", -12L, -1))
                .thenReturn(List.of(
                        "{\"role\":\"user\",\"content\":\"问题\"}", "{\"role\":\"assistant\",\"content\":\"回答\"}"));

        List<ChatTurn> turns = store.loadRecent("sess-1", 6);

        assertThat(turns).hasSize(2);
        assertThat(turns.getFirst()).isEqualTo(new ChatTurn("user", "问题"));
        assertThat(turns.get(1)).isEqualTo(new ChatTurn("assistant", "回答"));
    }

    @Test
    @DisplayName("会话为空/Redis 异常 -> 空列表（fail-open 丢记忆不阻塞）")
    void loadRecent_emptyOrError() {
        assertThat(store.loadRecent(null, 6)).isEmpty();
        assertThat(store.loadRecent("sess-x", 6)).isEmpty();
        when(listOps.range("eo:chat:session:sess-x", -12L, -1)).thenThrow(new RuntimeException("redis down"));
        assertThat(store.loadRecent("sess-x", 6)).isEmpty();
    }
}
