package com.cartethyia.easyorange.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "easyorange.ai")
public record AiProperties(
        DeepSeek deepseek,
        QwenVl qwenVl,
        @Valid Embedding embedding,
        Cache cache,
        RateLimit rateLimit,
        Budget budget,
        @Valid Eval eval,
        Routing routing,
        @Valid SemanticCache semanticCache,
        @Valid Chat chat) {

    public AiProperties {
        // 嵌套 record 在属性源里完全没有对应键时可能绑成 null，这里补上等价默认值 ——
        // 数值必须与各 record 上的 @DefaultValue 保持一致（改一处要改两处）
        if (deepseek == null) {
            deepseek = new DeepSeek(null, "https://api.deepseek.com", "deepseek-chat", 30000);
        }
        if (qwenVl == null) {
            qwenVl = new QwenVl(null, "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-vl-max", 60000);
        }
        if (embedding == null) {
            embedding = new Embedding(
                    null, "https://dashscope.aliyuncs.com/compatible-mode/v1", "text-embedding-v3", 1024, 30000);
        }
        if (cache == null) {
            cache = new Cache(5000, 24);
        }
        if (rateLimit == null) {
            rateLimit = new RateLimit(true, true);
        }
        if (budget == null) {
            budget = new Budget(true, Map.of());
        }
        if (eval == null) {
            eval = new Eval(false, "0 0 3 * * ?", 50, false, "0 15 3 * * ?");
        }
        if (routing == null) {
            routing = new Routing("chatModel", Map.of());
        }
        if (semanticCache == null) {
            semanticCache = new SemanticCache(true, 0.92, 200, 24);
        }
        if (chat == null) {
            chat = new Chat(24, 6, 5);
        }
    }

    public record DeepSeek(
            String apiKey,
            @DefaultValue("https://api.deepseek.com") String baseUrl,
            @DefaultValue("deepseek-chat") String model,
            @DefaultValue("30000") int timeout) {}

    public record QwenVl(
            String apiKey,

            @DefaultValue("https://dashscope.aliyuncs.com/compatible-mode/v1")
            String baseUrl,

            @DefaultValue("qwen-vl-max") String model,
            @DefaultValue("60000") int timeout) {}

    /**
     * Embedding 模型配置 — 走 OpenAI 兼容托管 API（DashScope text-embedding-v3）。
     * <p>
     * 维度（dimensions=1024）必须与 ES 索引 {@code dense_vector} 映射维度一致，
     * 否则语义搜索 kNN 查询会因维度不匹配失败。
     */
    public record Embedding(
            String apiKey,

            @DefaultValue("https://dashscope.aliyuncs.com/compatible-mode/v1")
            String baseUrl,

            @DefaultValue("text-embedding-v3") String model,
            @Min(1) @DefaultValue("1024") int dimensions,
            @DefaultValue("30000") int timeout) {}

    /**
     * LLM 故障降级缓存（本地 Caffeine）— 成功回答写入，LLM 调用失败时返回旧结果兜底。
     */
    public record Cache(
            @DefaultValue("5000") int staleMaxSize,
            @DefaultValue("24") int staleExpireHours) {}

    public record RateLimit(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("true") boolean failOpen) {}

    /**
     * Token 预算治理配置 — 按场景限制单次调用 token 上限 + 日预算上限。
     * <p>
     * 场景键与 {@link com.cartethyia.easyorange.ai.domain.constant.AiCallScope} 枚举名对齐
     * （pricing / review / copy / auto_listing / semantic / qa）。
     * 注解 {@code @TokenBudget} 上的字段为默认兜底值，配置文件可覆盖。
     */
    public record Budget(
            @DefaultValue("true") boolean enabled, @Valid Map<String, ScenarioBudget> scenarios) {

        public Budget {
            scenarios = Map.copyOf(scenarios == null ? Map.of() : scenarios);
        }

        /**
         * 查找场景预算配置，不存在返回 null（调用方应回退到注解默认值）。
         */
        public ScenarioBudget resolve(String scenario) {
            return scenarios.get(scenario);
        }

        public record ScenarioBudget(
                @DefaultValue("2000") int maxTokensPerCall,
                @DefaultValue("500000") int dailyTokenLimit) {}
    }

    /**
     * LLM-as-Judge 离线评估配置 — 定时对 eo_ai_call_log 中未评审的成功调用打分（1-5 + 评语）。
     * <p>
     * 回答「怎么判断 AI 输出质量」：输出质量从「感觉还行」变成「可量化、可回归」。
     *
     * @param enabled 是否启用离线评估
     * @param cron 评估任务调度表达式
     * @param batchSize 单轮评估的最大样本数
     * @param retrievalEnabled 是否启用 RAG 检索指标回归（hit@5 / MRR）— 仅需 embedding，不需要 LLM 生成
     * @param retrievalCron 检索指标回归任务的调度表达式
     */
    public record Eval(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("0 0 3 * * ?") String cron,
            @Min(1) @DefaultValue("50") int batchSize,
            @DefaultValue("false") boolean retrievalEnabled,
            @DefaultValue("0 15 3 * * ?") String retrievalCron) {}

    /**
     * 模型路由配置 — 按场景把调用分给不同模型 bean（对话走文本模型 / 图片分析走视觉模型）。
     * <p>
     * 键为场景名（如 chat_tool / vision / judge），值为 Spring bean 名；未配置的场景回退 {@code defaultModel}。
     * 已接入：chat_tool → chatModel（工具决策）、vision → visionChatModel（图片分析）、
     * judge → chatModel（LLM-as-Judge 评审，指向独立评审模型即可消除自评偏差）。
     * 接入新模型仅需在 {@code easyorange.ai.routing.scenarios} 里把场景指向新 bean 名，代码零改动。
     */
    public record Routing(@DefaultValue("chatModel") String defaultModel, Map<String, String> scenarios) {

        public Routing {
            scenarios = Map.copyOf(scenarios == null ? Map.of() : scenarios);
        }
    }

    /**
     * 语义缓存配置 — Embedding 相似度命中即复用历史回答（跨用户、近似问题共享），
     * 同时是「成本优化」的落地：相同意图的问题不再重复调 LLM。
     *
     * @param enabled 是否启用语义缓存
     * @param similarityThreshold 余弦相似度命中阈值（0.92 表示高度近义问题命中）
     * @param maxEntries 每个 scope 最多缓存的条目数，超出淘汰最旧条目。
     *     命中判定要遍历全部条目（Redis Hash 全量拉取 + 逐条算余弦），所以这个数直接决定
     *     未命中时的查询开销：500 条 ≈ 每次拉回 2MB 数据。200 是「够用又不拖慢未命中」的折中；
     *     若确实需要更大容量，应换成向量索引（ES kNN）而不是继续加大 Hash。
     * @param ttlHours 缓存条目 TTL（小时）
     */
    public record SemanticCache(
            @DefaultValue("true") boolean enabled,

            @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.92")
            double similarityThreshold,

            @DefaultValue("200") int maxEntries,
            @DefaultValue("24") int ttlHours) {}

    /**
     * 多轮对话记忆与 Agent 循环配置 — Redis 会话窗口（短期记忆）+ 画像注入（长期记忆）+ 多步工具循环上限。
     *
     * @param sessionTtlHours 会话 TTL（小时），过期即遗忘短期记忆
     * @param historyLimit 注入 prompt 的历史轮数（最近 N 轮）
     * @param maxSteps 多步 ReAct 循环的单次上限（含 finish 轮；达到上限未收敛则用已积累观察强制生成）
     */
    public record Chat(
            @DefaultValue("24") int sessionTtlHours,
            @DefaultValue("6") int historyLimit,
            @Min(1) @Max(10) @DefaultValue("5") int maxSteps) {}
}
