package com.cartethyia.easyorange.message.service.config;

import com.cartethyia.easyorange.framework.cache.RedisCache;
import com.cartethyia.easyorange.message.domain.repository.MessageSubscriptionRepository;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
import com.cartethyia.easyorange.message.domain.service.MessageRoutingService;
import com.cartethyia.easyorange.message.domain.service.OfflineMessageStoreService;
import com.cartethyia.easyorange.message.domain.service.RateLimiterService;
import com.cartethyia.easyorange.message.domain.service.SensitiveWordFilterService;
import com.cartethyia.easyorange.message.websocket.WebSocketNotifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageDomainServiceConfig {

    @Bean
    public MessageRoutingService messageRoutingService(
            MessageSubscriptionRepository subscriptionRepository,
            WebSocketNotifier sessionManager) {
        return new MessageRoutingService(subscriptionRepository, sessionManager);
    }

    @Bean
    public OfflineMessageStoreService offlineMessageStoreService(
            OfflineMessageRepository offlineMessageRepository) {
        return new OfflineMessageStoreService(offlineMessageRepository);
    }

    @Bean
    public RateLimiterService rateLimiterService(RedisCache redisCache) {
        return new RateLimiterService(redisCache);
    }

    @Bean
    public SensitiveWordFilterService sensitiveWordFilterService() {
        return new SensitiveWordFilterService();
    }
}