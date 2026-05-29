package com.cartethyia.easyorange.framework.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "easyorange.cache")
public class CacheProperties {

    private ImageCache image = new ImageCache();

    private L1Cache l1 = new L1Cache();

    @Getter
    @Setter
    public static class ImageCache {
        private int maxSize = 1000;
        private int expireHours = 24;
    }

    @Getter
    @Setter
    public static class L1Cache {
        private int maxSize = 5000;
        private int expireMinutes = 10;
    }
}