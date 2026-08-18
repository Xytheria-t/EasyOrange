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
    private Eval eval = new Eval();
    private Routing routing = new Routing();
    private SemanticCache semanticCache = new SemanticCache();
    private Chat chat = new Chat();

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

    /**
     * LLM 故障降级缓存（本地 Caffeine）— 成功回答写入，LLM 调用失败时返回旧结果兜底。
     */
    @Data
    public static class Cache {
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

    /**
     * LLM-as-Judge 离线评估配置 — 定时对 eo_ai_call_log 中未评审的成功调用打分（1-5 + 评语）。
     * <p>
     * 回答「怎么判断 AI 输出质量」：输出质量从「感觉还行」变成「可量化、可回归」。
     */
    @Data
    public static class Eval {
        private boolean enabled = false;
        private String cron = "0 0 3 * * ?";
        private int batchSize = 50;
        /** RAG 检索指标回归（hit@5 / MRR）— 仅需 embedding，不需要 LLM 生成。 */
        private boolean retrievalEnabled = false;

        private String retrievalCron = "0 15 3 * * ?";
    }

    /**
     * 模型路由配置 — 按场景把调用分给不同模型 bean（对话走文本模型 / 图片分析走视觉模型）。
     * <p>
     * 键为场景名（如 chat_tool / vision），值为 Spring bean 名；未配置的场景回退 {@link #defaultModel}。
     * 已接入：chat_tool → chatModel（工具决策）、vision → visionChatModel（图片分析）。
     * 接入新模型仅需在 {@code easyorange.ai.routing.scenarios} 里把场景指向新 bean 名，代码零改动。
     */
    @Data
    public static class Routing {
        private String defaultModel = "chatModel";
        private Map<String, String> scenarios = new HashMap<>();
    }

    /**
     * 语义缓存配置 — Embedding 相似度命中即复用历史回答（跨用户、近似问题共享），
     * 同时是「成本优化」的落地：相同意图的问题不再重复调 LLM。
     */
    @Data
    public static class SemanticCache {
        private boolean enabled = true;
        /** 余弦相似度命中阈值（0.92 表示高度近义问题命中）。 */
        private double similarityThreshold = 0.92;
        /** 每个 scope 最多缓存的条目数，超出淘汰最旧条目。 */
        private int maxEntries = 500;

        private int ttlHours = 24;
    }

    /**
     * 多轮对话记忆配置 — Redis 会话窗口（短期记忆）+ 画像注入（长期记忆）。
     */
    @Data
    public static class Chat {
        /** 会话 TTL（小时），过期即遗忘短期记忆。 */
        private int sessionTtlHours = 24;
        /** 注入 prompt 的历史轮数（最近 N 轮）。 */
        private int historyLimit = 6;
    }
}
