package com.cartethyia.easyorange.ai.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "easyorange.ai")
public class AiProperties {

    private DeepSeek deepseek = new DeepSeek();
    private QwenVl qwenVl = new QwenVl();
    private Embedding embedding = new Embedding();
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
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String model = "qwen-vl-max";
        private int timeout = 60000;
    }

    /**
     * Embedding 模型配置 — 走 OpenAI 兼容托管 API（DashScope text-embedding-v3）。
     * <p>
     * 维度（dimensions=1024）必须与 ES 索引 {@code dense_vector} 映射维度一致，
     * 否则语义搜索 kNN 查询会因维度不匹配失败。
     */
    @Data
    public static class Embedding {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String model = "text-embedding-v3";
        private int dimensions = 1024;
        private int timeout = 30000;
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
