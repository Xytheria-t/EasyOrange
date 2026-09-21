package com.cartethyia.easyorange.ai.adapter.outbound.cache;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.domain.port.ChatSessionPort;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 多轮对话短期记忆 — Redis List 会话窗口（TTL 24h，最近 N 轮），
 * 与 {@link com.cartethyia.easyorange.ai.application.chat.AiChatService} 的「最近 N 轮 + 工具结果」注入配合。
 * <p>
 * 一轮对话（提问 + 回答）一次 {@code RPUSH} + 一次裁剪 + 一次续期：两次单条写入会把 Redis 往返翻倍，
 * 且中间失败会留下只有提问没有回答的半轮记忆。
 * <p>
 * Redis 不可用 / 会话为空时返回空列表（fail-open：丢记忆不阻塞回答）；单条记录读不出来只跳过该条
 * —— 一条脏数据不该让整段对话记忆作废。
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class ChatSessionStore implements ChatSessionPort {

    private static final String KEY_PREFIX = "eo:chat:session:";

    /** 一轮对话 = 提问 + 回答两条 —— 存储按条裁剪 / 读取，配置按轮计。 */
    private static final int TURNS_PER_EXCHANGE = 2;

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    /**
     * 保存一轮对话（提问 + 回答），并裁剪到最近 N 轮 + 刷新 TTL。
     */
    @Override
    public void saveTurns(String sessionId, List<ChatTurn> turns) {
        if (sessionId == null || sessionId.isBlank() || turns == null || turns.isEmpty()) {
            return;
        }
        var redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            String key = KEY_PREFIX + sessionId;
            List<String> payloads =
                    turns.stream().map(objectMapper::writeValueAsString).toList();
            redis.opsForList().rightPushAll(key, payloads);
            redis.opsForList().trim(key, -entries(aiProperties.chat().historyLimit()), -1);
            redis.expire(key, Duration.ofHours(aiProperties.chat().sessionTtlHours()));
        } catch (Exception e) {
            log.warn("action=chat_session_save_failed, sessionId={}", sessionId, e);
        }
    }

    /**
     * 读取最近 N 轮对话（不含当前问题）。
     */
    @Override
    public List<ChatTurn> loadRecent(String sessionId, int limit) {
        if (sessionId == null || sessionId.isBlank()) {
            return List.of();
        }
        var redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return List.of();
        }
        List<String> raw;
        try {
            raw = redis.opsForList().range(KEY_PREFIX + sessionId, -entries(limit), -1);
        } catch (Exception e) {
            log.warn("action=chat_session_load_failed, sessionId={}", sessionId, e);
            return List.of();
        }
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        var turns = new ArrayList<ChatTurn>(raw.size());
        for (String json : raw) {
            try {
                ChatTurn turn = objectMapper.readValue(json, ChatTurn.class);
                if (turn != null && turn.content() != null) {
                    turns.add(turn);
                }
            } catch (Exception e) {
                log.warn("action=chat_turn_skipped, sessionId={}, reason={}", sessionId, e.getMessage());
            }
        }
        return turns;
    }

    /** 轮数 → Redis List 元素数（存储按条，配置按轮）。 */
    private static long entries(int rounds) {
        return Math.max(rounds, 1) * (long) TURNS_PER_EXCHANGE;
    }
}
