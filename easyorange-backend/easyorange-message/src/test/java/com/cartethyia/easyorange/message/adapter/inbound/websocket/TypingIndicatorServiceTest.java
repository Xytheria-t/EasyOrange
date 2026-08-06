package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
@DisplayName("TypingIndicatorService 单元测试")
class TypingIndicatorServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TypingIndicatorService typingIndicatorService;

    private static final String CONVERSATION_ID = "conv_1_2";
    private static final String USER_ID = "1";

    @Nested
    @DisplayName("setTyping")
    class SetTypingTests {

        @Test
        @DisplayName("正常设置正在输入状态")
        void setTyping_normal_setsWithTTL() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            typingIndicatorService.setTyping(CONVERSATION_ID, USER_ID);

            verify(valueOperations).set("chat:typing:" + CONVERSATION_ID + ":" + USER_ID, "1", 10L, TimeUnit.SECONDS);
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

            typingIndicatorService.setTyping("conv_99_100", "99");

            verify(valueOperations).set(eq("chat:typing:conv_99_100:99"), anyString(), anyLong(), any(TimeUnit.class));
        }
    }
}
