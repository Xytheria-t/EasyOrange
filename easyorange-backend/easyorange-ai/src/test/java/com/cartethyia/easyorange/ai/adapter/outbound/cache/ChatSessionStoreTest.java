package com.cartethyia.easyorange.ai.adapter.outbound.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.testsupport.PropertyBindings;
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
        // lenient：空会话 / 空轮次的 no-op 用例不碰 Redis
        lenient().when(redis.opsForList()).thenReturn(listOps);
        lenient().when(redisProvider.getIfAvailable()).thenReturn(redis);
        store = new ChatSessionStore(redisProvider, new ObjectMapper(), PropertyBindings.bind(AiProperties.class));
    }

    @Test
    @DisplayName("一轮对话 -> 两条一并右推 + 一次裁剪 + 一次续期")
    void saveTurns() {
        store.saveTurns("sess-1", List.of(ChatTurn.user("你好"), ChatTurn.assistant("在的")));

        // 小写 role code 与既有 Redis 数据同形（枚举名只用于 Java 侧）
        verify(listOps)
                .rightPushAll(
                        "eo:chat:session:sess-1",
                        List.of(
                                "{\"role\":\"user\",\"content\":\"你好\"}",
                                "{\"role\":\"assistant\",\"content\":\"在的\"}"));
        verify(listOps).trim("eo:chat:session:sess-1", -12L, -1);
        verify(redis).expire("eo:chat:session:sess-1", Duration.ofHours(24));
    }

    @Test
    @DisplayName("空会话 ID / 空轮次 -> 不碰 Redis")
    void saveTurns_noop() {
        store.saveTurns("  ", List.of(ChatTurn.user("你好")));
        store.saveTurns("sess-1", List.of());

        verifyNoInteractions(listOps);
        verify(redis, never()).expire(anyString(), any());
    }

    @Test
    @DisplayName("读取最近轮次 -> 反序列化为 ChatTurn 列表")
    void loadRecent() {
        when(listOps.range("eo:chat:session:sess-1", -12L, -1))
                .thenReturn(List.of(
                        "{\"role\":\"user\",\"content\":\"问题\"}", "{\"role\":\"assistant\",\"content\":\"回答\"}"));

        List<ChatTurn> turns = store.loadRecent("sess-1");

        assertThat(turns).containsExactly(ChatTurn.user("问题"), ChatTurn.assistant("回答"));
    }

    @Test
    @DisplayName("单条记录损坏 / 角色未知 -> 只跳过该条，整段记忆不作废")
    void loadRecent_skipsUnreadableTurn() {
        when(listOps.range("eo:chat:session:sess-1", -12L, -1))
                .thenReturn(List.of(
                        "{\"role\":\"user\",\"content\":\"问题\"}",
                        "{ 截断的 JSON",
                        "{\"role\":\"system\",\"content\":\"角色未知\"}",
                        "{\"role\":\"assistant\",\"content\":\"回答\"}"));

        List<ChatTurn> turns = store.loadRecent("sess-1");

        assertThat(turns).containsExactly(ChatTurn.user("问题"), ChatTurn.assistant("回答"));
    }

    @Test
    @DisplayName("会话为空/Redis 异常 -> 空列表（fail-open 丢记忆不阻塞）")
    void loadRecent_emptyOrError() {
        assertThat(store.loadRecent(null)).isEmpty();
        assertThat(store.loadRecent("sess-x")).isEmpty();
        when(listOps.range("eo:chat:session:sess-x", -12L, -1)).thenThrow(new RuntimeException("redis down"));
        assertThat(store.loadRecent("sess-x")).isEmpty();
    }
}
