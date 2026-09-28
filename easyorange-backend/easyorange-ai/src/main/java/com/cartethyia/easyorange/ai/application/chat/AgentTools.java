package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.retrieval.AssetSourcingService;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalService;
import com.cartethyia.easyorange.ai.domain.model.AssetComparison;
import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.PriceStats;
import com.cartethyia.easyorange.ai.domain.port.AssetDetailPort;
import com.cartethyia.easyorange.ai.domain.port.UserPreferenceRepository;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.execution.ToolCallResultConverter;

/**
 * Agent 循环的内部工具面 — 7 个工具的 schema 与执行都在这里：{@code @Tool} / {@code @ToolParam} 注解
 * 生成供应商侧校验的 JSON Schema，方法体即「执行 + 观察格式化」。每次循环实例化一份：召回物累加器是
 * 单次请求内的可变状态，由实例独占持有，编排器经只读快照读取；框架不执行这些工具，执行与循环控制权
 * 都在 {@link AgentLoopRunner}。
 * <p>
 * 约定：thought 是每个工具的必填参数（原生 tool calling 没有独立的「决策理由」通道，工具方法不消费，
 * 由 runner 取出落 trace / SSE）；抛异常 = 该步失败（runner 收敛成失败观察交回模型修复），「查无此资产」
 * 这类有效结果必须返回观察文本而不是抛异常；finish 只有 schema 没有执行（runner 在执行前按名称拦截）；
 * remember_preference 是唯一的写路径（按 userId + key 幂等 upsert，作为独立工具让「写入长期记忆」成为
 * 模型自主决策的一步，步数超限 / 预算耗尽 / 决策失败三条降级路径下偏好不再静默丢失）。
 */
@SuppressWarnings("unused") // thought 只进工具 schema，方法体不消费（见类注释）
public class AgentTools {

    /** 工具名与 {@code @Tool(name = ...)} 同源，编排器引用常量而不是重写字面量。 */
    static final String TOOL_KNOWLEDGE_SEARCH = "knowledge_search";

    static final String TOOL_PRODUCT_SEARCH = "product_search";

    static final String TOOL_PRODUCT_DETAIL = "product_detail";

    static final String TOOL_MARKET_PRICE_STATS = "market_price_stats";

    static final String TOOL_COMPARE_ASSETS = "compare_assets";

    static final String TOOL_REMEMBER_PREFERENCE = "remember_preference";

    static final String TOOL_FINISH = "finish";

    /** 每轮工具召回的 topK（决策失败降级补检索沿用同一口径）。 */
    static final int RETRIEVAL_TOP_K = 5;

    static final int ASSET_TOP_K = 5;

    /** remember_preference 的偏好类别白名单 —— 取值与 schema 描述 / prompt yml 同源，落库前的代码层硬校验。 */
    private static final Set<String> PREFERENCE_KEYS = Set.of("condition", "price_range", "style", "location");

    /** 观察里列举的命中条数上限 —— 观察是给下一轮决策的摘要，召回多少条都只列举这么多。 */
    private static final int OBSERVATION_SUMMARY_LIMIT = 3;

    /**
     * 检索无新增时的观察文案 —— 收敛判据本身（见 {@link #knowledgeSearch} / {@link #productSearch}）：
     * 判据是「本轮命中的条目有多少此前已出现过」，模型不会自己看出「再查也是重复」，把这件事作为一条
     * 明确观察交回，比在循环里硬性拦掉更合适。
     */
    private static final String NO_NEW_HIT_OBSERVATION = "本次检索无新增信息（命中的内容此前已出现过）：请直接调用 finish 基于已有信息作答，不要再换关键词重试";

    /**
     * 判为「无新增」的重合比例阈值 —— 本轮命中里此前出现过的条目占比达到此值即收敛。
     * 只认「完全重复」不够：检索是 topK 截断的，换关键词常返回高度重叠但不完全相同的一批，
     * 逐条判重会让模型无限换词直到撞步数上限（实测 7 步里 5 步换词重搜）。取 0.6：首次检索重合率为 0，
     * 一次检索能带进 3 条以上新内容时（约 40% 重合）不算冗余。
     */
    private static final double REDUNDANT_OVERLAP_RATIO = 0.6;

    /** 详情描述进观察前截断的字数 —— 描述是自由文本，长度不可控。 */
    private static final int DETAIL_DESC_MAX_CHARS = 80;

    /** 各轮召回物累加器 — 请求内可变状态，实例独占持有；读取走只读快照方法，不交出可变引用。 */
    private final List<KnowledgeHit> knowledgeHits = new ArrayList<>();
    private final List<AssetHit> assets = new ArrayList<>();
    private final List<AssetDetail> details = new ArrayList<>();
    private final KnowledgeRetrievalService retrievalService;
    private final AssetSourcingService assetSourcingService;
    private final AssetDetailPort assetDetailPort;
    private final UserPreferenceRepository preferenceRepository;
    /** 画像归属用户；机器主体（评估跑批）为 null（长期记忆不落库）。 */
    @Nullable
    private final String userId;

    AgentTools(
            KnowledgeRetrievalService retrievalService,
            AssetSourcingService assetSourcingService,
            AssetDetailPort assetDetailPort,
            UserPreferenceRepository preferenceRepository,
            @Nullable String userId) {
        this.retrievalService = retrievalService;
        this.assetSourcingService = assetSourcingService;
        this.assetDetailPort = assetDetailPort;
        this.preferenceRepository = preferenceRepository;
        this.userId = userId;
    }

    /** 已累加的召回物（只读快照，供循环出口装配 Result）。 */
    List<KnowledgeHit> knowledgeHits() {
        return List.copyOf(knowledgeHits);
    }

    List<AssetHit> assets() {
        return List.copyOf(assets);
    }

    List<AssetDetail> details() {
        return List.copyOf(details);
    }

    @Tool(
            name = TOOL_KNOWLEDGE_SEARCH,
            description = "检索平台规则知识库（交易流程 / 退款 / 运费 / 禁售品类）",
            resultConverter = ObservationTextConverter.class)
    public String knowledgeSearch(
            @ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought,
            @ToolParam(description = "改写后的检索关键词，3-10 字") String query) {
        List<KnowledgeHit> found = retrievalService.search(query, RETRIEVAL_TOP_K);
        List<KnowledgeHit> fresh = retainNewKnowledge(found);
        knowledgeHits.addAll(fresh);
        return isRedundant(found.size() - fresh.size(), found.size())
                ? NO_NEW_HIT_OBSERVATION
                : summarizeKnowledge(found);
    }

    @Tool(name = TOOL_PRODUCT_SEARCH, description = "检索在售资产（找货 / 比价）", resultConverter = ObservationTextConverter.class)
    public String productSearch(
            @ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought,
            @ToolParam(description = "改写后的找货关键词，3-10 字，保留品类与硬约束（预算 / 成色）") String query) {
        List<AssetHit> found = assetSourcingService.search(query, ASSET_TOP_K);
        List<AssetHit> fresh = retainNewAssets(found);
        assets.addAll(fresh);
        return isRedundant(found.size() - fresh.size(), found.size()) ? NO_NEW_HIT_OBSERVATION : summarizeAssets(found);
    }

    @Tool(
            name = TOOL_PRODUCT_DETAIL,
            description = "查看某件在售资产的详情（描述 / 成色 / 位置 / 卖家）",
            resultConverter = ObservationTextConverter.class)
    public String productDetail(
            @ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought,
            @ToolParam(description = "资产 ID，必须取自此前 product_search 观察中方括号里的资产 ID") String productId) {
        if (isBlank(productId)) {
            throw new IllegalArgumentException("缺少 productId，无法查询资产详情");
        }
        Optional<AssetDetail> found = findDetail(productId.trim());
        if (found.isEmpty()) {
            // 「查无此资产」是有效结果而非故障：返回观察文本让模型换目标，不打断对话
            return "未找到该资产（可能不存在或已下架）";
        }
        details.add(found.get());
        return summarizeDetail(found.get());
    }

    @Tool(
            name = TOOL_MARKET_PRICE_STATS,
            description = "对已召回的资产算行情（在售件数 / 均价 / 价格区间），用于判断某件值不值得买；零模型计算",
            resultConverter = ObservationTextConverter.class)
    public String marketPriceStats(@ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought) {
        return PriceStats.of(assets)
                .map(PriceStats::observation)
                .orElse("暂无可统计的在售资产（尚未召回，或召回项均无有效价格），先调用 product_search 召回候选");
    }

    @Tool(
            name = TOOL_COMPARE_ASSETS,
            description = "对 2-4 件候选做逐维确定性比对（价格 / 成色 / 地区 / 在售状态），一次替代多次 product_detail",
            resultConverter = ObservationTextConverter.class)
    public String compareAssets(
            @ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought,
            @ToolParam(description = "要对比的资产 ID 列表，2-4 个，必须取自此前 product_search 观察中方括号里的资产 ID")
                    List<String> productIds) {
        List<String> requested = productIds == null
                ? List.of()
                : productIds.stream()
                        .filter(id -> !isBlank(id))
                        .map(String::strip)
                        .distinct()
                        .toList();
        if (requested.size() < AssetComparison.MIN_CANDIDATES) {
            return "至少需要 2 个资产 ID 才能对比，请从 product_search 的观察里取候选 ID";
        }
        List<String> targets = requested.size() > AssetComparison.MAX_CANDIDATES
                ? requested.subList(0, AssetComparison.MAX_CANDIDATES)
                : requested;
        List<AssetDetail> found = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        // 走批量端口：一次 3 条查询取回全部候选，与数量无关（逐个 findDetail 会放大成 3N）
        Map<String, AssetDetail> byId = new HashMap<>();
        for (AssetDetail detail : findDetails(targets)) {
            byId.put(detail.productId(), detail);
        }
        for (String id : targets) {
            AssetDetail detail = byId.get(id);
            if (detail != null) {
                found.add(detail);
                details.add(detail);
            } else {
                missing.add(id);
            }
        }
        // 部分 ID 查不到不是失败：把查不到的点出来，模型据此换 ID 重试或按剩余候选下结论
        String missingNote = missing.isEmpty() ? "" : "；未找到：%s".formatted(String.join("、", missing));
        return AssetComparison.of(found)
                .map(comparison -> comparison.observation() + missingNote)
                .orElse("可用于对比的资产不足 2 件（可能不存在或已下架）" + missingNote);
    }

    @Tool(
            name = TOOL_REMEMBER_PREFERENCE,
            description = "记录用户的长期偏好（成色 / 价格区间 / 风格 / 地区）到用户画像，跨会话生效；对话中出现明确偏好时调用一次即可，同一偏好不要重复记录",
            resultConverter = ObservationTextConverter.class)
    public String rememberPreference(
            @ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought,
            @ToolParam(description = "偏好类别，只允许 condition（成色）/ price_range（价格区间）/ style（风格）/ location（地区）")
                    String preferenceKey,
            @ToolParam(description = "偏好的具体值（如「九五新」「5000 以内」「复古」）") String preferenceValue) {
        if (isBlank(preferenceKey) || isBlank(preferenceValue)) {
            // 「模型没提取到有效偏好」是有效结果而非故障：返回观察文本让模型继续，不打断对话
            return "偏好类别或取值为空，已跳过记录；直接继续回答即可";
        }
        if (userId == null) {
            return "机器调用不落长期画像，已跳过记录；直接继续回答即可";
        }
        String key = preferenceKey.trim();
        if (!PREFERENCE_KEYS.contains(key)) {
            // 白名单是代码层的硬校验：schema 描述与 prompt yml 只是对模型的指令，提示注入可让模型
            // 带任意 key 进来，落库前以本集合为准；拒绝理由回给模型，不当故障处理
            return "偏好类别仅支持 condition / price_range / style / location，已跳过记录；直接继续回答即可";
        }
        String value = preferenceValue.trim();
        try {
            preferenceRepository.record(userId, key, value);
        } catch (Exception e) {
            // DB 故障是真实故障，按「抛异常 = 该步失败」上报（runner 收敛成失败观察，模型可重试或忽略）
            throw new IllegalStateException("偏好记录失败: " + reasonOf(e), e);
        }
        return "已记录偏好：%s = %s".formatted(key, value);
    }

    @Tool(name = TOOL_FINISH, description = "信息已足够回答，或无需检索（寒暄 / 闲聊），不再调用任何工具")
    public String finish(@ToolParam(description = "收敛理由，不超过 20 字的中文概括") String thought) {
        // 方法体不会被执行：runner 在执行前按名称拦截，这里只为让 finish 出现在发给供应商的工具 schema 里
        return TOOL_FINISH;
    }

    /** 决策失败降级的补检索 — 判重口径与 {@link #knowledgeSearch} 一致，降级路径不会把 Result 撑出重复来源。 */
    void recallKnowledgeFallback(String question) {
        knowledgeHits.addAll(retainNewKnowledge(retrievalService.search(question, RETRIEVAL_TOP_K)));
    }

    /** 按 ID 查详情 — product_detail 与 compare_assets 共用：端口抛出（DB 故障）按工具失败上报，empty（查无此资产）是正常结果。 */
    private Optional<AssetDetail> findDetail(String productId) {
        return detailQuery(() -> assetDetailPort.findDetail(productId));
    }

    /** compare_assets 的批量通道，失败语义与 {@link #findDetail} 一致（抛 = 本步失败）。 */
    private List<AssetDetail> findDetails(List<String> productIds) {
        return detailQuery(() -> assetDetailPort.findDetails(productIds));
    }

    /** 详情端口调用的统一失败口径：端口异常（DB 故障）包成 {@code IllegalStateException} 上报为该步失败。 */
    private <T> T detailQuery(Supplier<T> query) {
        try {
            return query.get();
        } catch (Exception e) {
            throw new IllegalStateException("资产详情查询失败: " + reasonOf(e), e);
        }
    }

    /** 本轮检索是否已无新增信息 —— 完全没召回到（{@code foundCount == 0}）不算冗余：那是空结果，不是重复。 */
    private static boolean isRedundant(int seenCount, int foundCount) {
        return foundCount > 0 && (double) seenCount / foundCount >= REDUNDANT_OVERLAP_RATIO;
    }

    /**
     * 保留本轮新增的召回物 —— 判据取 docId（资产按 productId），缺失时退回标题：
     * ES 命中必有 docId，兜底只为 LIKE 降级路径不因 null 误判成「全新增」。
     */
    private List<KnowledgeHit> retainNewKnowledge(List<KnowledgeHit> found) {
        Set<String> seen = knowledgeHits.stream().map(AgentTools::knowledgeKey).collect(Collectors.toSet());
        return found.stream().filter(hit -> seen.add(knowledgeKey(hit))).collect(Collectors.toList());
    }

    /** 同 {@link #retainNewKnowledge}，资产按 productId 判重。 */
    private List<AssetHit> retainNewAssets(List<AssetHit> found) {
        Set<String> seen = assets.stream().map(AgentTools::assetKey).collect(Collectors.toSet());
        return found.stream().filter(asset -> seen.add(assetKey(asset))).collect(Collectors.toList());
    }

    private static String knowledgeKey(KnowledgeHit hit) {
        return isBlank(hit.docId()) ? String.valueOf(hit.title()) : hit.docId();
    }

    private static String assetKey(AssetHit asset) {
        return isBlank(asset.productId()) ? String.valueOf(asset.title()) : asset.productId();
    }

    private static String summarizeKnowledge(List<KnowledgeHit> found) {
        if (found.isEmpty()) {
            return "知识库未命中，可换关键词重试或直接 finish";
        }
        String titles = found.stream()
                .limit(OBSERVATION_SUMMARY_LIMIT)
                .map(KnowledgeHit::title)
                .collect(Collectors.joining(" / "));
        return "命中 %d 条：%s".formatted(found.size(), titles);
    }

    private static String summarizeAssets(List<AssetHit> found) {
        if (found.isEmpty()) {
            return "在售资产未召回，可换更宽泛的关键词重试或直接 finish";
        }
        String items = found.stream()
                .limit(OBSERVATION_SUMMARY_LIMIT)
                .map(asset -> "[%s] %s ¥%s"
                        .formatted(
                                asset.productId(),
                                asset.title(),
                                asset.price() == null
                                        ? "面议"
                                        : asset.price().stripTrailingZeros().toPlainString()))
                .collect(Collectors.joining("；"));
        return "召回 %d 件：%s".formatted(found.size(), items);
    }

    private static String summarizeDetail(AssetDetail detail) {
        return "描述：%s｜成色：%s｜位置：%s｜卖家：%s｜状态：%s"
                .formatted(
                        ellipsis(detail.description()),
                        orDefault(detail.conditionDesc(), "未标注"),
                        orDefault(detail.location(), "未知"),
                        orDefault(detail.sellerName(), "未知"),
                        orDefault(detail.status(), "未知"));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String orDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static String ellipsis(String value) {
        String text = orDefault(value, "无");
        return text.length() > DETAIL_DESC_MAX_CHARS ? text.substring(0, DETAIL_DESC_MAX_CHARS) + "…" : text;
    }

    private static String reasonOf(Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    /** 观察文本原样返回 — 默认转换器会把 String 返回值 JSON 序列化（观察多一层引号），本工具面的观察是进下一轮 prompt 的纯文本。finish 无执行体、不需要该转换器。 */
    @NullMarked
    public static final class ObservationTextConverter implements ToolCallResultConverter {

        @Override
        public String convert(@Nullable Object result, @Nullable Type returnType) {
            return result == null ? "" : String.valueOf(result);
        }
    }
}
