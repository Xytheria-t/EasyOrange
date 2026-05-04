package com.cartethyia.easyorange.framework.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainEventPersistenceService {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.application.name:easyorange}")
    private String applicationName;

    private static final String EVENT_KEY_PREFIX = "domain:event:";
    private static final int EVENT_TTL_DAYS = 7;

    public void persist(BaseDomainEvent event) {
        try {
            String eventKey = EVENT_KEY_PREFIX + applicationName + ":" + event.getEventId();
            String eventJson = objectMapper.writeValueAsString(event);

            redisTemplate.opsForValue().set(
                    eventKey,
                    eventJson,
                    java.time.Duration.ofDays(EVENT_TTL_DAYS)
            );

            String typeKey = EVENT_KEY_PREFIX + "type:" + event.eventType();
            redisTemplate.opsForZSet().add(typeKey, event.getEventId(), System.currentTimeMillis());

        } catch (Exception e) {
            log.error("事件持久化失败：eventId={}", event.getEventId(), e);
        }
    }

    public void persistAll(List<BaseDomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (BaseDomainEvent event : events) {
            persist(event);
        }
    }
}