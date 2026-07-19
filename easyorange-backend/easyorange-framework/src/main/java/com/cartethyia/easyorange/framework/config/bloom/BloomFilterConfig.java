package com.cartethyia.easyorange.framework.config.bloom;

import com.cartethyia.easyorange.framework.bloom.BloomFilter;
import com.cartethyia.easyorange.framework.bloom.RedisBitmapBloomFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

@AutoConfiguration
public class BloomFilterConfig {

    @Bean
    public BloomFilter bloomFilter(RedisTemplate<Object, Object> redisTemplate) {
        return new RedisBitmapBloomFilter(redisTemplate);
    }
}
