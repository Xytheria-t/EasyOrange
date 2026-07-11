package com.cartethyia.easyorange.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "easyorange.ai")
public class AiProperties {

    private DeepSeek deepseek = new DeepSeek();
    private QwenVl qwenVl = new QwenVl();
    private Cache cache = new Cache();
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class DeepSeek {
        private String apiKey;
        private String baseUrl = "https://api.deepseek.com";
        private String model = "deepseek-chat";
        private int timeout = 30000;
    }

    @Data
    public static class QwenVl {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        private String model = "qwen-vl-max";
        private int timeout = 60000;
    }

    @Data
    public static class Cache {
        private boolean enabled = true;
        private int l1MaxSize = 10000;
        private int l1ExpireMinutes = 5;
        private int staleMaxSize = 5000;
        private int staleExpireHours = 24;
    }

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private boolean failOpen = true;
    }
}