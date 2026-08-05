package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TypingIndicatorService {

    private static final String TYPING_KEY = "chat:typing:%s:%s";
    private static final long TYPING_TTL_SECONDS = 10;

    private final RedisTemplate<String, String> redisTemplate;

    public void setTyping(String conversationId, String userId) {
        if (conversationId == null || userId == null) {
            return;
        }
        String key = TYPING_KEY.formatted(conversationId, userId);
        redisTemplate.opsForValue().set(key, "1", TYPING_TTL_SECONDS, TimeUnit.SECONDS);
    }
}
