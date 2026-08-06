package com.cartethyia.easyorange.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "easyorange.cache")
public class CacheProperties {

    private ImageCache image = new ImageCache();

    private L1Cache l1 = new L1Cache();

    private L2Cache l2 = new L2Cache();

    @Data
    public static class ImageCache {
        private int maxSize = 1000;
        private int expireHours = 24;
    }

    @Data
    public static class L1Cache {
        private int maxSize = 5000;
        private int expireMinutes = 10;
    }

    @Data
    public static class L2Cache {
        private int expireMinutes = 30;
        private int negativeExpireSeconds = 30;
    }
}
