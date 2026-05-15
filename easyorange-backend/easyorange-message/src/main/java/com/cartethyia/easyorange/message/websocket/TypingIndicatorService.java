package com.cartethyia.easyorange.message.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TypingIndicatorService {

    private static final String TYPING_KEY = "chat:typing:%s:%s";
    private static final long TYPING_TTL_SECONDS = 10;

    private final RedisTemplate<String, String> redisTemplate;

    public void setTyping(String conversationId, Long userId) {
        if (conversationId == null || userId == null) {
            return;
        }
        String key = TYPING_KEY.formatted(conversationId, userId);
        redisTemplate.opsForValue().set(key, "1", TYPING_TTL_SECONDS, TimeUnit.SECONDS);
    }

    public Set<Long> getTypingUsers(String conversationId, Long excludeUserId) {
        if (conversationId == null) {
            return Collections.emptySet();
        }
        String pattern = TYPING_KEY.formatted(conversationId, "*");
        var keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptySet();
        }
        return keys.stream()
                .map(k -> {
                    String[] parts = k.split(":");
                    return parts.length >= 3 ? Long.valueOf(parts[2]) : null;
                })
                .filter(id -> id != null && !id.equals(excludeUserId))
                .collect(java.util.stream.Collectors.toSet());
    }

    public void removeTyping(String conversationId, Long userId) {
        if (conversationId == null || userId == null) {
            return;
        }
        String key = TYPING_KEY.formatted(conversationId, userId);
        redisTemplate.delete(key);
    }
}
