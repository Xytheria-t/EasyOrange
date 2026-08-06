package com.cartethyia.easyorange.message.application.config;

import com.cartethyia.easyorange.framework.util.DistributedRateLimiter;
import com.cartethyia.easyorange.message.application.service.OfflineMessageStoreService;
import com.cartethyia.easyorange.message.application.service.RateLimiterService;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
import com.cartethyia.easyorange.message.domain.service.SensitiveWordFilterService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageDomainConfig {

    @Bean
    public OfflineMessageStoreService offlineMessageStoreService(OfflineMessageRepository offlineMessageRepository) {
        return new OfflineMessageStoreService(offlineMessageRepository);
    }

    @Bean
    public RateLimiterService rateLimiterService(DistributedRateLimiter distributedRateLimiter) {
        return new RateLimiterService(distributedRateLimiter);
    }

    @Bean
    public SensitiveWordFilterService sensitiveWordFilterService() {
        return new SensitiveWordFilterService();
    }
}
