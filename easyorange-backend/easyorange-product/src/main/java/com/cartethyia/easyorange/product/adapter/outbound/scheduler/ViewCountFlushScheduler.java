package com.cartethyia.easyorange.product.adapter.outbound.scheduler;

import com.cartethyia.easyorange.product.application.service.ViewCountFlushAppService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountFlushScheduler {

    private final RedisTemplate<Object, Object> redisTemplate;
    private final ViewCountFlushAppService viewCountFlushService;

    private static final String VIEW_COUNT_LOCK = "eo:product:views:lock";

    @Scheduled(initialDelay = 15000, fixedRate = 5000)
    public void flush() {
        var locked = redisTemplate.opsForValue().setIfAbsent(VIEW_COUNT_LOCK, "1", 10, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) return;

        try {
            viewCountFlushService.flush();
        } catch (Exception e) {
            log.error("action=flushViewCountFailed", e);
        } finally {
            redisTemplate.delete(VIEW_COUNT_LOCK);
        }
    }
}
