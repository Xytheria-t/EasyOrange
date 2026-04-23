package com.cartethyia.easyorange.framework.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 领域事件发布器实现
 * 支持同步和异步发布，并持久化事件到 Redis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventPublisherImpl implements DomainEventPublisher {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${spring.application.name:easyorange}")
    private String applicationName;
    
    private static final String EVENT_KEY_PREFIX = "domain:event:";
    private static final int EVENT_TTL_DAYS = 7;

    @Override
    public void publish(BaseDomainEvent event) {
        try {
            // 1. 同步发布事件
            log.info("发布领域事件：type={} eventId={}", event.eventType(), event.getEventId());
            
            // 2. 持久化事件到 Redis（用于事件追踪和重放）
            persistEvent(event);
            
        } catch (Exception e) {
            log.error("发布领域事件失败：type={} eventId={}", event.eventType(), event.getEventId(), e);
            // 事件发布失败不影响主流程，只记录日志
        }
    }

    @Override
    public void publishAll(List<BaseDomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        
        for (BaseDomainEvent event : events) {
            publish(event);
        }
    }
    
    /**
     * 异步发布事件（用于非关键业务场景）
     */
    @Async("domainEventExecutor")
    public void publishAsync(BaseDomainEvent event) {
        publish(event);
    }
    
    /**
     * 持久化事件到 Redis
     */
    private void persistEvent(BaseDomainEvent event) {
        try {
            String eventKey = EVENT_KEY_PREFIX + applicationName + ":" + event.getEventId();
            String eventJson = objectMapper.writeValueAsString(event);
            
            redisTemplate.opsForValue().set(
                    eventKey,
                    eventJson,
                    java.time.Duration.ofDays(EVENT_TTL_DAYS)
            );
            
            // 同时按事件类型索引，方便查询
            String typeKey = EVENT_KEY_PREFIX + "type:" + event.eventType();
            redisTemplate.opsForZSet().add(typeKey, event.getEventId(), System.currentTimeMillis());
            
            log.debug("事件持久化成功：eventId={}", event.getEventId());
            
        } catch (Exception e) {
            log.error("事件持久化失败：eventId={}", event.getEventId(), e);
        }
    }
}
