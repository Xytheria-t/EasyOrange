package com.cartethyia.easyorange.framework.event.idempotency;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 事件消费幂等检查器 — 基于 Redis {@code SET NX EX}（Spring 的 {@code setIfAbsent}）原子标记，
 * 保证同一事件（{@code consumerId:eventType + eventId}）在去重窗口内只有一个消费者执行一次。
 * <p>
 * 设计说明：
 * <ul>
 *   <li>标记即去重：单命令原子，无需分布式锁；并发投递 / 重复投递时只有一个消费者能拿到标记，其余跳过。</li>
 *   <li>去重窗口 24h（{@code DONE_TTL_HOURS}），需覆盖 Outbox 重投与 DLQ 重试周期。</li>
 *   <li>处理失败由 {@link EventConsumerHandler} 调用 {@link #unmark} 撤销标记，使容器重试 / DLQ 重投可重新处理；
 *       进程在标记后、处理完成前崩溃的残留窗口，由消费者业务副作用幂等兜底。</li>
 * </ul>
 */
@Slf4j
@Component
public class EventIdempotencyChecker {

    private static final String EVENT_DONE_PREFIX = "eo:event:done:";
    private static final long DONE_TTL_HOURS = 24;

    private final StringRedisTemplate redisTemplate;

    public EventIdempotencyChecker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试领取事件处理权：{@code true}=本消费者获得处理权，{@code false}=已处理或他人处理中（重复）。
     * <p>
     * 实现为一条原子 {@code SET NX EX}，无需「先查再写」两步，因此没有分布式锁的引入空间。
     */
    public boolean tryMark(String eventType, String eventId) {
        return Boolean.TRUE.equals(redisTemplate
                .opsForValue()
                .setIfAbsent(EVENT_DONE_PREFIX + eventType + ":" + eventId, "1", DONE_TTL_HOURS, TimeUnit.HOURS));
    }

    /**
     * 撤销处理标记（处理失败后调用，允许重试重新执行）；Redis 异常仅记日志，不掩盖原始失败。
     */
    public void unmark(String eventType, String eventId) {
        try {
            redisTemplate.delete(EVENT_DONE_PREFIX + eventType + ":" + eventId);
        } catch (Exception e) {
            log.error("Event idempotency unmark failed: eventType={}, eventId={}", eventType, eventId, e);
        }
    }
}
