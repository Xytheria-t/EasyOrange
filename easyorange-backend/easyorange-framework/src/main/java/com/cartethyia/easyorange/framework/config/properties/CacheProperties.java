package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 缓存配置。
 *
 * @param image 图片处理结果本地缓存（Caffeine，按最后访问时间过期）
 * @param defaultTtl Spring Cache 统一 TTL — 一致性靠写路径显式 evict，TTL 仅作兜底（默认 30 分钟）
 * @param ttlJitter TTL 随机抖动比例（0~1）— 写入时按比例加随机偏移错峰过期防雪崩（0 关闭，默认 0.1）
 */
@Validated
@ConfigurationProperties(prefix = "easyorange.cache")
public record CacheProperties(
        @Valid ImageCache image,
        @DefaultValue("30m") Duration defaultTtl,

        @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.1")
        double ttlJitter) {

    public CacheProperties {
        if (image == null) {
            image = new ImageCache(ImageCache.DEFAULT_MAX_SIZE, ImageCache.DEFAULT_EXPIRE_HOURS);
        }
    }

    /**
     * 图片处理结果缓存。
     *
     * @param maxSize 最大条目数
     * @param expireHours 最后一次访问后多久过期（小时）
     */
    public record ImageCache(
            @Min(1) @DefaultValue(DEFAULT_MAX_SIZE + "") int maxSize,
            @Min(1) @DefaultValue(DEFAULT_EXPIRE_HOURS + "") int expireHours) {

        private static final int DEFAULT_MAX_SIZE = 1000;
        private static final int DEFAULT_EXPIRE_HOURS = 24;
    }
}
