package com.cartethyia.easyorange.ai.adapter.outbound.cache;

import com.cartethyia.easyorange.ai.chat.ChatTurn;
import com.cartethyia.easyorange.ai.config.AiProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 多轮对话短期记忆 — Redis List 会话窗口（TTL 24h，最近 N 轮），
 * 与 {@link com.cartethyia.easyorange.ai.service.AiChatService} 的「最近 N 轮 + 工具结果」注入配合。
 * <p>
 * Redis 不可用 / 会话为空时返回空列表（fail-open：丢记忆不阻塞回答）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionStore {

    private static final String KEY_PREFIX = "eo:chat:session:";

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    /**
     * 保存一轮对话，并裁剪到最近 N 轮 + 刷新 TTL。
     */
    public void saveTurn(String sessionId, String role, String content) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        var redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            String key = KEY_PREFIX + sessionId;
            redis.opsForList().rightPush(key, objectMapper.writeValueAsString(new ChatTurn(role, content)));
            int keepTurns = Math.max(aiProperties.getChat().getHistoryLimit(), 1);
            redis.opsForList().trim(key, -keepTurns * 2L, -1);
            redis.expire(key, Duration.ofHours(aiProperties.getChat().getSessionTtlHours()));
        } catch (Exception e) {
            log.warn("Save chat turn failed, memory lost for this turn", e);
        }
    }

    /**
     * 读取最近 N 轮对话（不含当前问题）。
     */
    public List<ChatTurn> loadRecent(String sessionId, int limit) {
        if (sessionId == null || sessionId.isBlank()) {
            return List.of();
        }
        var redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return List.of();
        }
        try {
            List<String> raw = redis.opsForList().range(KEY_PREFIX + sessionId, -limit * 2L, -1);
            if (raw == null || raw.isEmpty()) {
                return List.of();
            }
            var turns = new ArrayList<ChatTurn>(raw.size());
            for (String json : raw) {
                ChatTurn turn = objectMapper.readValue(json, ChatTurn.class);
                if (turn != null && turn.content() != null) {
                    turns.add(turn);
                }
            }
            return turns;
        } catch (Exception e) {
            log.warn("Load chat turns failed, treating as empty", e);
            return List.of();
        }
    }
}
