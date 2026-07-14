package com.cartethyia.easyorange.framework.config.bloom;

import com.cartethyia.easyorange.framework.bloom.BloomFilter;
import com.cartethyia.easyorange.framework.bloom.RedisBitmapBloomFilter;
import com.cartethyia.easyorange.framework.cache.RedisCache;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class BloomFilterConfig {

    @Bean
    public BloomFilter bloomFilter(RedisCache redisCache) {
        return new RedisBitmapBloomFilter(redisCache);
    }
}
