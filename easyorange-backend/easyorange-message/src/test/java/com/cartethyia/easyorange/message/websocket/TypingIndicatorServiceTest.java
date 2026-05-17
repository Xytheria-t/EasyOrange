package com.cartethyia.easyorange.message.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TypingIndicatorService 单元测试")
class TypingIndicatorServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TypingIndicatorService typingIndicatorService;

    private static final String CONVERSATION_ID = "conv_1_2";
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Nested
    @DisplayName("setTyping")
    class SetTypingTests {

        @Test
        @DisplayName("正常设置正在输入状态")
        void setTyping_normal_setsWithTTL() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            typingIndicatorService.setTyping(CONVERSATION_ID, USER_ID);

            verify(valueOperations).set(
                    "chat:typing:" + CONVERSATION_ID + ":" + USER_ID,
                    "1",
                    10L,
                    TimeUnit.SECONDS
            );
        }

        @Test
        @DisplayName("conversationId 为 null 时直接返回")
        void setTyping_nullConversationId_returnsEarly() {
            typingIndicatorService.setTyping(null, USER_ID);

            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("userId 为 null 时直接返回")
        void setTyping_nullUserId_returnsEarly() {
            typingIndicatorService.setTyping(CONVERSATION_ID, null);

            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("两个参数都为 null 时直接返回")
        void setTyping_bothNull_returnsEarly() {
            typingIndicatorService.setTyping(null, null);

            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("Key 格式正确")
        void setTyping_correctKeyFormat() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            typingIndicatorService.setTyping("conv_99_100", 99L);

            verify(valueOperations).set(eq("chat:typing:conv_99_100:99"), anyString(), anyLong(), any(TimeUnit.class));
        }
    }

    @Nested
    @DisplayName("getTypingUsers")
    class GetTypingUsersTests {

        @Test
        @DisplayName("返回正在输入的用户，排除指定用户")
        void getTypingUsers_returnsUsersExcluding() {
            Set<String> keys = Set.of(
                    "chat:typing:" + CONVERSATION_ID + ":" + USER_ID,
                    "chat:typing:" + CONVERSATION_ID + ":" + OTHER_USER_ID
            );
            when(redisTemplate.keys(anyString())).thenReturn(keys);

            Set<Long> result = typingIndicatorService.getTypingUsers(CONVERSATION_ID, USER_ID);

            assertThat(result)
                    .hasSize(1)
                    .containsExactly(OTHER_USER_ID);
        }

        @Test
        @DisplayName("没有正在输入的用户时返回空集合")
        void getTypingUsers_noKeys_returnsEmpty() {
            when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

            Set<Long> result = typingIndicatorService.getTypingUsers(CONVERSATION_ID, USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("keys 返回 null 时返回空集合")
        void getTypingUsers_nullKeys_returnsEmpty() {
            when(redisTemplate.keys(anyString())).thenReturn(null);

            Set<Long> result = typingIndicatorService.getTypingUsers(CONVERSATION_ID, USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("conversationId 为 null 时返回空集合")
        void getTypingUsers_nullConversationId_returnsEmpty() {
            Set<Long> result = typingIndicatorService.getTypingUsers(null, USER_ID);

            assertThat(result).isEmpty();
            verify(redisTemplate, never()).keys(anyString());
        }

        @Test
        @DisplayName("正确构建 redis key pattern")
        void getTypingUsers_correctKeyPattern() {
            when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

            typingIndicatorService.getTypingUsers(CONVERSATION_ID, USER_ID);

            verify(redisTemplate).keys("chat:typing:" + CONVERSATION_ID + ":*");
        }

        @Test
        @DisplayName("排除所有用户时返回空集合")
        void getTypingUsers_excludeAllUsers_returnsEmpty() {
            Set<String> keys = Set.of(
                    "chat:typing:" + CONVERSATION_ID + ":" + USER_ID
            );
            when(redisTemplate.keys(anyString())).thenReturn(keys);

            Set<Long> result = typingIndicatorService.getTypingUsers(CONVERSATION_ID, USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("跳过格式错误的 key")
        void getTypingUsers_skipsMalformedKeys() {
            Set<String> keys = Set.of(
                    "invalid:key:format",
                    "chat:typing:" + CONVERSATION_ID + ":" + OTHER_USER_ID
            );
            when(redisTemplate.keys(anyString())).thenReturn(keys);

            Set<Long> result = typingIndicatorService.getTypingUsers(CONVERSATION_ID, USER_ID);

            assertThat(result)
                    .hasSize(1)
                    .containsExactly(OTHER_USER_ID);
        }

        @Test
        @DisplayName("解析用户 ID 失败时跳过")
        void getTypingUsers_skipsNonNumericUserId() {
            Set<String> keys = Set.of(
                    "chat:typing:" + CONVERSATION_ID + ":abc"
            );
            when(redisTemplate.keys(anyString())).thenReturn(keys);

            Set<Long> result = typingIndicatorService.getTypingUsers(CONVERSATION_ID, USER_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("removeTyping")
    class RemoveTypingTests {

        @Test
        @DisplayName("正常移除正在输入状态")
        void removeTyping_normal_deletes() {
            typingIndicatorService.removeTyping(CONVERSATION_ID, USER_ID);

            verify(redisTemplate).delete("chat:typing:" + CONVERSATION_ID + ":" + USER_ID);
        }

        @Test
        @DisplayName("conversationId 为 null 时直接返回")
        void removeTyping_nullConversationId_returnsEarly() {
            typingIndicatorService.removeTyping(null, USER_ID);

            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("userId 为 null 时直接返回")
        void removeTyping_nullUserId_returnsEarly() {
            typingIndicatorService.removeTyping(CONVERSATION_ID, null);

            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("两个参数都为 null 时直接返回")
        void removeTyping_bothNull_returnsEarly() {
            typingIndicatorService.removeTyping(null, null);

            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("删除时 key 格式正确")
        void removeTyping_correctKeyFormat() {
            typingIndicatorService.removeTyping("conv_99_100", 99L);

            verify(redisTemplate).delete("chat:typing:conv_99_100:99");
        }
    }
}
