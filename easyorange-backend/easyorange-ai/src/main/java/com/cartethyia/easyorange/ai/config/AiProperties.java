package com.cartethyia.easyorange.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "easyorange.ai")
public class AiProperties {

    private DeepSeek deepseek = new DeepSeek();
    private QwenVl qwenVl = new QwenVl();
    private Cache cache = new Cache();
    private RateLimit rateLimit = new RateLimit();
    private Budget budget = new Budget();

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

    /**
     * Token 预算治理配置 — 按场景限制单次调用 token 上限 + 日预算上限。
     * <p>
     * 场景键与 {@link com.cartethyia.easyorange.ai.enums.AiCallScope} 枚举名对齐
     * （pricing / review / copy / auto_listing / semantic / qa）。
     * 注解 {@code @TokenBudget} 上的字段为默认兜底值，配置文件可覆盖。
     */
    @Data
    public static class Budget {
        private boolean enabled = true;
        private Map<String, ScenarioBudget> scenarios = new HashMap<>();

        /**
         * 查找场景预算配置，不存在返回 null（调用方应回退到注解默认值）。
         */
        public ScenarioBudget resolve(String scenario) {
            return scenarios.get(scenario);
        }

        @Data
        public static class ScenarioBudget {
            private int maxTokensPerCall = 2000;
            private int dailyTokenLimit = 500_000;
        }
    }
}
