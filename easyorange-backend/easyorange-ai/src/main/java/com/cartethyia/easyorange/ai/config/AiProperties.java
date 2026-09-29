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
        Text text,
        Vision vision,
        @Valid Embedding embedding,
        Cache cache,
        RateLimit rateLimit,
        Budget budget,
        Routing routing,
        @Valid SemanticCache semanticCache,
        @Valid Chat chat) {

    public AiProperties {
        // 嵌套 record 在属性源里完全没有对应键时可能绑成 null，补等价默认值；数值须与 application.yaml 保持一致（yaml 是唯一主源，此处仅兜底）
        // 两边失同步的症状不报错、也不打日志：单测用 PropertyBindings 裸绑（不加载 application.yaml）时会静默吃这里的旧值，
        // 表现为「本地测试全绿、起服行为不同」。embedding 的 dimensions 尤其致命 —— 与 ES dense_vector 不一致时
        // kNN 全库召回，表现为「语义检索能跑但结果很烂」，排查成本极高。
        if (text == null) {
            text = new Text(null, "https://api.deepseek.com", "deepseek-chat", "", 30000);
        }
        if (vision == null) {
            vision = new Vision(null, "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-vl-max", 60000);
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
        if (routing == null) {
            routing = new Routing("chatModel", Map.of());
        }
        if (semanticCache == null) {
            semanticCache = new SemanticCache(true, 0.92, 200, 24);
        }
        if (chat == null) {
            chat = new Chat(24, 6, 7, 2000, 90);
        }
    }

    /**
     * 文本生成模型配置 — 供应商无关，槽位按<b>职责</b>命名而非按厂商：
     * 厂商只作为 base-url / model 两个配置<b>值</b>存在，写进键名会让「键名是 deepseek、实际跑百炼」
     * 这种失配在排查时无法一眼看出（默认值仍是 DeepSeek 官方端点，可不带任何环境变量直接跑通）。
     *
     * @param routerModel 工具决策专用模型，留空与 {@code model} 同模型；决策与生成为什么分开配
     *     见 {@link AiModelConfig#decisionChatModel}
     * <p>
     * 端点 / 模型 / 超时的单一来源是 application.yaml；构造器兜底仅防属性源整段缺失（测试裸绑场景），
     * 不另设 @DefaultValue——同一默认值写两处必然漂移。
     */
    public record Text(String apiKey, String baseUrl, String model, String routerModel, int timeout) {}

    /** 视觉理解模型配置 — 同 {@link Text}，槽位命名只表职责。 */
    public record Vision(String apiKey, String baseUrl, String model, int timeout) {}

    /** Embedding 模型配置 — dimensions 必须与 ES 索引 {@code dense_vector} 映射维度一致，否则语义检索 kNN 查询维度不匹配失败。 */
    public record Embedding(String apiKey, String baseUrl, String model, int dimensions, int timeout) {}

    /** LLM 故障降级缓存（本地 Caffeine）— 成功回答写入，LLM 调用失败时返回旧结果兜底。 */
    public record Cache(
            @DefaultValue("5000") int staleMaxSize,
            @DefaultValue("24") int staleExpireHours) {}

    public record RateLimit(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("true") boolean failOpen) {}

    /**
     * Token 预算治理配置 — 按场景限制单次调用 token 上限 + 日预算上限。
     * 场景键与 {@link com.cartethyia.easyorange.ai.domain.enums.AiCallScope} 枚举名对齐；
     * {@code @TokenBudget} 注解上的字段为默认兜底值，配置文件可覆盖。
     */
    public record Budget(
            @DefaultValue("true") boolean enabled, @Valid Map<String, ScenarioBudget> scenarios) {

        public Budget {
            scenarios = Map.copyOf(scenarios == null ? Map.of() : scenarios);
        }

        /** 查找场景预算配置，不存在返回 null（调用方应回退到注解默认值）。 */
        public ScenarioBudget resolve(String scenario) {
            return scenarios.get(scenario);
        }

        public record ScenarioBudget(
                @DefaultValue("2000") int maxTokensPerCall,
                @DefaultValue("500000") int dailyTokenLimit) {}
    }

    /**
     * 模型路由配置 — 键为场景名（chat_tool / vision / judge），值为 Spring bean 名，
     * 未配置的场景回退 {@code defaultModel}；接入新模型只改配置代码零改动，
     * judge 指向独立评审模型即可消除自评偏差。
     */
    public record Routing(@DefaultValue("chatModel") String defaultModel, Map<String, String> scenarios) {

        public Routing {
            scenarios = Map.copyOf(scenarios == null ? Map.of() : scenarios);
        }
    }

    /**
     * 语义缓存配置 — Embedding 相似度命中即复用历史回答（跨用户、近似问题共享，相同意图不再重复调 LLM）。
     *
     * @param maxEntries 每个 scope 最多缓存的条目数：命中判定要遍历全部条目（Redis Hash 全量拉取 +
     *     逐条算余弦），这个数直接决定未命中时的查询开销（500 条 ≈ 每次拉回 2MB），需更大容量应换
     *     向量索引（ES kNN）而不是继续加大 Hash
     */
    public record SemanticCache(
            @DefaultValue("true") boolean enabled,

            @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.92")
            double similarityThreshold,

            @DefaultValue("200") int maxEntries,
            @DefaultValue("24") int ttlHours) {}

    /**
     * 多轮对话记忆与 工具调用循环配置 — Redis 会话窗口（短期记忆）+ 画像注入（长期记忆）+ 循环上限。
     *
     * @param maxSteps 多步 ReAct 循环单次上限（含 finish 轮）：典型轨迹 search → 计算/详情 → remember
     *     → finish 需 4~5 步，工具面扩到 5 个后由 5 上调至 7 留余量，避免工具变多反而更容易撞上限降级
     * @param maxHistoryTokens 历史注入 prompt 的 token 预算（估算口径见 TokenEstimator），轮数窗口
     *     之上的第二道裁剪；&lt;=0 关闭。超限只裁历史、不影响生成（生成侧由 maxTokensPerCall 兜底）
     * @param sessionLockWaitSeconds 同会话串行锁的获取等待上限（秒）：同会话 load→loop→save 非原子，
     *     并发请求会互相串写历史；上限需覆盖最坏 7 轮富轨迹的端到端耗时（实测 62s），超时按
     *     「会话处理中」业务提示返回。<b>application.yaml 目前没有这个键</b>，只有这里的默认值生效 ——
     *     运维要调它得先在 yaml 里补 {@code easyorange.ai.chat.session-lock-wait-seconds}
     */
    public record Chat(
            @DefaultValue("24") int sessionTtlHours,
            @DefaultValue("6") int historyLimit,
            @Min(1) @Max(10) @DefaultValue("7") int maxSteps,
            @DefaultValue("2000") int maxHistoryTokens,
            @DefaultValue("90") int sessionLockWaitSeconds) {}
}
