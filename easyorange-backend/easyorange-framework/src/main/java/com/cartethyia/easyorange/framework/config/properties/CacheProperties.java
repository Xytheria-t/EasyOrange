package com.cartethyia.easyorange.framework.config.properties;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "easyorange.cache")
public class CacheProperties {

    private ImageCache image = new ImageCache();

    /** Spring Cache 统一 TTL — 一致性靠写路径显式 evict，TTL 仅作兜底（默认 30 分钟） */
    private Duration defaultTtl = Duration.ofMinutes(30);

    /** TTL 随机抖动比例（0~1）— 写入时按比例加随机偏移错峰过期防雪崩（0 关闭，默认 0.1） */
    private double ttlJitter = 0.1;

    @Data
    public static class ImageCache {
        private int maxSize = 1000;
        private int expireHours = 24;
    }
}
