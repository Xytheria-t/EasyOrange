package com.cartethyia.easyorange.message.application.config;

import com.cartethyia.easyorange.message.application.service.OfflineMessageStoreService;
import com.cartethyia.easyorange.message.application.service.RateLimiterService;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
import com.cartethyia.easyorange.message.domain.service.SensitiveWordFilterService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class MessageDomainConfig {

    @Bean
    public OfflineMessageStoreService offlineMessageStoreService(
            OfflineMessageRepository offlineMessageRepository) {
        return new OfflineMessageStoreService(offlineMessageRepository);
    }

    @Bean
    public RateLimiterService rateLimiterService(RedisTemplate<Object, Object> redisTemplate) {
        return new RateLimiterService(redisTemplate);
    }

    @Bean
    public SensitiveWordFilterService sensitiveWordFilterService() {
        return new SensitiveWordFilterService();
    }
}
