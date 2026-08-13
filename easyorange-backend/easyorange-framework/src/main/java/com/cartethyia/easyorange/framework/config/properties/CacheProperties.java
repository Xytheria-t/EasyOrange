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

    @Data
    public static class ImageCache {
        private int maxSize = 1000;
        private int expireHours = 24;
    }
}
