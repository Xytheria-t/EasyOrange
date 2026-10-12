package com.cartethyia.easyorange.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * AI 模块全部可调项的绑定点（前缀 {@code easyorange.ai}），取值语义就近写在各槽位 record 上。
 * <p>
 * 默认值三层：application.yaml 是唯一主源；{@code @DefaultValue} 兜「部分键缺失」，紧凑构造器兜
 * 「整段缺失」——空属性源不会实例化嵌套 record（实测绑成 null），不补则裸绑测试直接拿到空值。
 * 同一数值因此在 yaml 与代码里各存一份，失同步不报错（症状是「单测全绿、起服行为不同」）；
 * 高代价字段（dimensions ↔ 两份 ES 映射）另有一致性测试哨兵，不靠注释自觉。
 * <p>
 * 嵌套约束经 {@code @Valid} 级联、只挂给带约束的槽位；{@code @Validated} 让非法值在绑定期即失败。
 */
@Validated
@ConfigurationProperties(prefix = "easyorange.ai")
public record AiProperties(
        Text text,
        Vision vision,
        Judge judge,
        @Valid Embedding embedding,
        StaleCache staleCache,
        RateLimit rateLimit,
        Budget budget,
        Routing routing,
        @Valid SemanticCache semanticCache,
        @Valid Chat chat,
        @Valid Listing listing) {

    public AiProperties {
        // 属性源整段缺失时的兜底 —— 数值须与 application.yaml 保持同步
        if (text == null) {
            text = new Text(null, "https://api.deepseek.com", "deepseek-chat", "", 30000);
        }
        if (vision == null) {
            vision = new Vision(null, "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-vl-max", 60000);
        }
        if (judge == null) {
            judge = new Judge(null, "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus", 60000);
        }
        if (embedding == null) {
            embedding = new Embedding(
                    null, "https://dashscope.aliyuncs.com/compatible-mode/v1", "text-embedding-v3", 1024, 30000);
        }
        if (staleCache == null) {
            staleCache = new StaleCache(5000, 24);
        }
        if (rateLimit == null) {
            rateLimit = new RateLimit(true, true);
        }
        if (budget == null) {
            budget = new Budget(true, Map.of());
        }
        if (routing == null) {
            routing = new Routing("chatModel", Map.of());
        }
        if (semanticCache == null) {
            semanticCache = new SemanticCache(true, 0.92, 200, 24);
        }
        if (chat == null) {
            chat = new Chat(24, 6, 7, 2000, 90);
        }
        if (listing == null) {
            listing = new Listing(6, List.of());
        }
    }

    /**
     * 文本生成模型配置 — 供应商无关，槽位按职责命名而非按厂商：厂商只作为 base-url / model 两个值存在，
     * 写进键名会让「键名是 deepseek、实际跑百炼」在排查时无法一眼看出（兜底值仍是 DeepSeek 官方端点，
     * 不带任何环境变量可直接跑通）。本槽不另设 {@code @DefaultValue}（测试不按单键覆盖它），
     * 端点 / 模型 / 超时在代码里只有构造器兜底这一处。
     *
     * @param routerModel 工具决策专用模型，留空与 {@code model} 同模型；分槽理由见 {@link AiModelConfig#decisionChatModel}
     */
    public record Text(String apiKey, String baseUrl, String model, String routerModel, int timeout) {}

    /** 视觉理解模型配置 — 同 {@link Text}，槽位命名只表职责。 */
    public record Vision(String apiKey, String baseUrl, String model, int timeout) {}

    /**
     * 评审模型配置（LLM-as-Judge）— 生成默认 DeepSeek、评审默认百炼 qwen；apiKey 缺省借视觉槽的
     * DashScope key（yaml 占位符兜底），要独立计费时另设。分家族理由见 {@link AiModelConfig#judgeChatModel}。
     */
    public record Judge(String apiKey, String baseUrl, String model, int timeout) {}

    /**
     * Embedding 模型配置 — {@code dimensions} 必须与 ES 索引 {@code dense_vector} 映射维度一致，否则
     * 语义检索 kNN 查询维度不匹配；{@code @Min} 只在绑定期拦非正数，与两份映射的对齐另有一致性测试兜底。
     */
    public record Embedding(
            String apiKey,
            String baseUrl,
            String model,
            @Min(1) int dimensions,
            int timeout) {}

    /** LLM 故障降级缓存（本地 Caffeine）— 成功回答写入，LLM 调用失败时返回旧结果兜底。 */
    public record StaleCache(
            @DefaultValue("5000") int maxSize,
            @DefaultValue("24") int expireHours) {}

    /** AI 接口限流配置 — enabled 控拦截器注册；failOpen 是 Redis 故障时的闸门：放行（默认）还是上抛。 */
    public record RateLimit(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("true") boolean failOpen) {}

    /**
     * Token 预算治理配置 — 每场景限单次调用 token 上限 + 日预算上限；场景键与
     * {@link com.cartethyia.easyorange.ai.domain.enums.AiCallScope#budgetScenario()} 同源，yaml 缺场景时回退
     * 注解 {@code @TokenBudget} 的默认值（两侧字面量的一致性由 TokenBudgetScenarioContractTest 反射钉住）。
     */
    public record Budget(@DefaultValue("true") boolean enabled, Map<String, ScenarioBudget> scenarios) {

        public Budget {
            scenarios = Map.copyOf(scenarios == null ? Map.of() : scenarios);
        }

        /** 场景不存在返回 null——调用方应回退到注解默认值。 */
        public ScenarioBudget resolve(String scenario) {
            return scenarios.get(scenario);
        }

        /**
         * 单场景上限 —— 0 的语义是「关」不是「耗尽」：{@code dailyTokenLimit <= 0} 表示不限日，
         * {@code maxTokensPerCall = 0} 表示不设供应商侧 max_tokens；判定式单点在
         * {@link com.cartethyia.easyorange.ai.domain.model.TokenBudgetPolicy#exhausted}。
         */
        public record ScenarioBudget(
                @DefaultValue("2000") int maxTokensPerCall,
                @DefaultValue("500000") int dailyTokenLimit) {}
    }

    /**
     * 模型路由配置 — 键为场景名（常量定义见 {@link AiModelRouter}），值为 Spring bean 名；未配置的场景
     * 回退 {@code defaultModel}，换模型只改配置代码零改动，judge 指向独立评审模型即可消除自评偏差。
     */
    public record Routing(@DefaultValue("chatModel") String defaultModel, Map<String, String> scenarios) {

        public Routing {
            scenarios = Map.copyOf(scenarios == null ? Map.of() : scenarios);
        }
    }

    /**
     * 语义缓存配置 — Embedding 相似度命中即复用历史回答（按用户分桶，相同意图不再重复调 LLM）。
     *
     * @param maxEntries 每个用户 × scope 桶的条目上限：命中判定要全量拉回 Redis Hash 逐条算余弦（单条 ≈ 向量
     *     5.5KB + 序列化响应，500 条 ≈ 每次拉回 3MB+），直接决定未命中时的查询开销；更大容量应换
     *     ES kNN 向量索引而不是继续加大 Hash
     */
    public record SemanticCache(
            @DefaultValue("true") boolean enabled,

            @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.92")
            double similarityThreshold,

            @DefaultValue("200") int maxEntries,
            @DefaultValue("24") int ttlHours) {}

    /**
     * 多轮对话记忆与工具调用循环配置 — Redis 会话窗口（短期记忆）+ 偏好注入（长期记忆）+ 循环上限。
     *
     * @param maxRounds 多步 ReAct 循环单次上限（含 finish 轮）：典型轨迹 search → 计算/详情 → remember
     *     → finish 需 4~5 步，取 7 留一轮换词 / 多意图追问的余量——上限过紧会逼富轨迹中途强制收敛
     * @param maxHistoryTokens 历史注入 prompt 的 token 预算（估算口径见 TokenEstimator），轮数窗口
     *     之上的第二道裁剪；&lt;=0 关闭。超限只裁历史、不影响生成（生成侧由 maxTokensPerCall 兜底）
     * @param sessionLockWaitSeconds 同会话串行锁的获取等待上限（秒）：同会话 load→loop→save 非原子，
     *     并发请求会互相串写历史；上限需覆盖最坏 7 轮富轨迹的端到端耗时（实测 62s），超时按
     *     「会话处理中」业务提示返回
     */
    public record Chat(
            @DefaultValue("24") int sessionTtlHours,
            @DefaultValue("6") int historyLimit,
            @Min(1) @Max(10) @DefaultValue("7") int maxRounds,
            @DefaultValue("2000") int maxHistoryTokens,
            @DefaultValue("90") int sessionLockWaitSeconds) {}

    /**
     * 发布链路（多步工具循环）配置。
     *
     * @param maxRounds 发布循环单次上限（含 finish 轮）：典型轨迹 禁售查证 → 品类召回 → 行情统计 → finish
     *     需 4 步，取 6 留出检索换词一轮的余量；比 chat 少一档是因为发布链路的检索词由预识别线索锚定，
     *     不存在买家侧多意图追问把轮数撑长的形态
     * @param allowedImageHosts 取图主机白名单（精确匹配主机名，大小写不敏感）——命中的主机跳过内网地址
     *     校验，默认空只放行公网；自建图床落内网才需要开，也是本地 HttpServer 用例能跑通真实下载路径的唯一入口
     */
    public record Listing(
            @Min(1) @Max(10) @DefaultValue("6") int maxRounds, List<String> allowedImageHosts) {

        public Listing {
            allowedImageHosts = allowedImageHosts == null ? List.of() : List.copyOf(allowedImageHosts);
        }
    }
}
