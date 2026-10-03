package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.retrieval.AssetSourcingAppService;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalAppService;
import com.cartethyia.easyorange.ai.application.support.FailureReason;
import com.cartethyia.easyorange.ai.application.support.RetrievalObservations;
import com.cartethyia.easyorange.ai.domain.model.AssetComparison;
import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.port.AssetDetailPort;
import com.cartethyia.easyorange.ai.domain.port.UserPreferenceRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 多步工具循环的内部工具面 — 7 个工具的 schema 与执行都在这里（{@code @Tool} / {@code @ToolParam} 注解生成供应商侧校验
 * 的 JSON Schema）；每次循环实例化一份，召回物累加器是单次请求内的可变状态、由实例独占。
 * <p>
 * 约定：thought 是每个工具的必填参数（原生 tool calling 没有独立的「决策理由」通道，只由编排器取出落 trace / SSE）；抛
 * 异常 = 该步失败，「查无此资产」这类有效结果必须返回观察文本；finish 只有 schema 没有执行；remember_preference 是唯一
 * 写路径（按 userId + key 幂等 upsert），独立成一步是为了让降级路径下偏好不再静默丢失。
 */
@SuppressWarnings("unused") // thought 只进工具 schema，方法体不消费（见类注释）
public class ChatTools {

    /** 工具名与 {@code @Tool(name = ...)} 同源，编排器引用常量；{@code eval/golden-set.yaml} 的 {@code expected_tools}
     * 按这些名字在加载期强校验，工具改名后标注写错会炸而不是静默评成「路由走错」。 */
    public static final String TOOL_KNOWLEDGE_SEARCH = "knowledge_search";

    public static final String TOOL_PRODUCT_SEARCH = "product_search";

    public static final String TOOL_PRODUCT_DETAIL = "product_detail";

    public static final String TOOL_MARKET_PRICE_STATS = "market_price_stats";

    public static final String TOOL_COMPARE_ASSETS = "compare_assets";

    public static final String TOOL_REMEMBER_PREFERENCE = "remember_preference";

    /** 偏好值的长度上限 —— 超长值不落库（跨会话回注的画像块不吃无界文本）。 */
    static final int PREFERENCE_VALUE_MAX_LENGTH = 60;

    public static final String TOOL_FINISH = "finish";

    public static final Set<String> TOOL_NAMES = Set.of(
            TOOL_KNOWLEDGE_SEARCH,
            TOOL_PRODUCT_SEARCH,
            TOOL_PRODUCT_DETAIL,
            TOOL_MARKET_PRICE_STATS,
            TOOL_COMPARE_ASSETS,
            TOOL_REMEMBER_PREFERENCE,
            TOOL_FINISH);

    private static final Set<String> PREFERENCE_KEYS = Set.of("condition", "price_range", "style", "location");

    /** 详情描述进观察前截断的字数 —— 描述是自由文本，长度不可控。 */
    private static final int DETAIL_DESC_MAX_CHARS = 80;

    /** 各轮召回物累加器 — 请求内可变状态，实例独占持有；读取走只读快照方法，不交出可变引用。 */
    private final List<KnowledgeHit> knowledgeHits = new ArrayList<>();

    private final List<AssetHit> assetHits = new ArrayList<>();
    private final List<AssetDetail> details = new ArrayList<>();
    private final KnowledgeRetrievalAppService retrievalService;
    private final AssetSourcingAppService assetSourcingService;
    private final AssetDetailPort assetDetailPort;
    private final UserPreferenceRepository preferenceRepository;
    /** 画像归属用户；机器主体（评估跑批）为 null（长期记忆不落库）。 */
    @Nullable
    private final String userId;

    ChatTools(
            KnowledgeRetrievalAppService retrievalService,
            AssetSourcingAppService assetSourcingService,
            AssetDetailPort assetDetailPort,
            UserPreferenceRepository preferenceRepository,
            @Nullable String userId) {
        this.retrievalService = retrievalService;
        this.assetSourcingService = assetSourcingService;
        this.assetDetailPort = assetDetailPort;
        this.preferenceRepository = preferenceRepository;
        this.userId = userId;
    }

    List<KnowledgeHit> knowledgeHits() {
        return List.copyOf(knowledgeHits);
    }

    List<AssetHit> assetHits() {
        return List.copyOf(assetHits);
    }

    List<AssetDetail> details() {
        return List.copyOf(details);
    }

    @Tool(
            name = TOOL_KNOWLEDGE_SEARCH,
            description = "检索平台规则知识库（交易流程 / 退款 / 运费 / 禁售品类）",
            resultConverter = RetrievalObservations.ObservationTextConverter.class)
    public String searchKnowledge(
            @ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought,
            @ToolParam(description = "改写后的检索关键词，3-10 字") String query) {
        List<KnowledgeHit> found = retrievalService.search(query, RetrievalObservations.TOP_K);
        var turn = RetrievalObservations.knowledgeTurn(knowledgeHits, found);
        knowledgeHits.addAll(turn.fresh());
        return turn.observation();
    }

    @Tool(
            name = TOOL_PRODUCT_SEARCH,
            description = "检索在售资产（找货 / 比价）",
            resultConverter = RetrievalObservations.ObservationTextConverter.class)
    public String searchProducts(
            @ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought,
            @ToolParam(description = "改写后的找货关键词，3-10 字，保留品类与硬约束（预算 / 成色）") String query) {
        List<AssetHit> found = assetSourcingService.search(query, RetrievalObservations.TOP_K);
        var turn = RetrievalObservations.assetTurn(assetHits, found);
        assetHits.addAll(turn.fresh());
        return turn.observation();
    }

    @Tool(
            name = TOOL_PRODUCT_DETAIL,
            description = "查看某件在售资产的详情（描述 / 成色 / 位置 / 卖家）",
            resultConverter = RetrievalObservations.ObservationTextConverter.class)
    public String fetchProductDetail(
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
        return formatDetailObservation(found.get());
    }

    // 顺序约束写进 schema 描述而不只写 system prompt：模型是在选工具时读它的，等生成回答时才发现约束已经晚一轮
    @Tool(
            name = TOOL_MARKET_PRICE_STATS,
            description = "对已召回的资产算行情（在售件数 / 均价 / 价格区间），用于判断某件值不值得买；零模型计算。"
                    + "必须在 product_search 之后的轮次调用（同一轮里它会排在 product_search 前面而统计到空集）",
            resultConverter = RetrievalObservations.ObservationTextConverter.class)
    public String summarizeMarketPrice(@ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought) {
        return RetrievalObservations.priceStatsObservation(assetHits);
    }

    @Tool(
            name = TOOL_COMPARE_ASSETS,
            description = "对 2-4 件候选做逐维确定性比对（价格 / 成色 / 地区 / 在售状态），一次替代多次 product_detail",
            resultConverter = RetrievalObservations.ObservationTextConverter.class)
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
            resultConverter = RetrievalObservations.ObservationTextConverter.class)
    public String rememberPreference(
            @ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought,
            @ToolParam(description = "偏好类别，只允许 condition（成色）/ price_range（价格区间）/ style（风格）/ location（地区）")
                    String preferenceKey,
            @ToolParam(description = "偏好的具体值（如「九五新」「5000 以内」「复古」）") String preferenceValue) {
        if (isBlank(preferenceKey) || isBlank(preferenceValue)) {
            return "偏好类别或取值为空，已跳过记录；直接继续回答即可";
        }
        if (userId == null) {
            return "机器调用不落长期画像，已跳过记录；直接继续回答即可";
        }
        String key = preferenceKey.trim();
        if (!PREFERENCE_KEYS.contains(key)) {
            // 白名单（取值与 schema 描述 / prompt yml 同源）是代码层硬校验：那两处只是对模型的指令，提示注入可让它带任意 key 进来
            return "偏好类别仅支持 condition / price_range / style / location，已跳过记录；直接继续回答即可";
        }
        String value = preferenceValue.trim();
        if (value.length() > PREFERENCE_VALUE_MAX_LENGTH) {
            // value 原样落库并经 <user_profile> 回注后续所有会话的画像块 —— 无长度约束时它就是
            // 一条跨会话的存储型注入通道，长度上限把「塞一段话进画像」的成本抬到不可用
            return "偏好取值过长（上限 60 字），请概括成短语再记；直接继续回答即可";
        }
        try {
            preferenceRepository.record(userId, key, value);
        } catch (Exception e) {
            // DB 故障是真实故障（区别于「查无此资产」那类有效结果），按抛异常 = 该步失败上报
            throw new IllegalStateException("偏好记录失败: " + FailureReason.of(e), e);
        }
        return "已记录偏好：%s = %s".formatted(key, value);
    }

    @Tool(name = TOOL_FINISH, description = "信息已足够回答，或无需检索（寒暄 / 闲聊），不再调用任何工具")
    public String finish(@ToolParam(description = "收敛理由，不超过 20 字的中文概括") String thought) {
        // 方法体永不执行，这里只为让 finish 出现在发给供应商的工具 schema 里
        return TOOL_FINISH;
    }

    /** 决策失败降级的补检索 — 判重口径与 {@link #searchKnowledge} 一致，降级路径不会把 Result 撑出重复来源。 */
    void searchKnowledgeForFallback(String question) {
        List<KnowledgeHit> found = retrievalService.search(question, RetrievalObservations.TOP_K);
        knowledgeHits.addAll(RetrievalObservations.freshKnowledge(knowledgeHits, found));
    }

    private Optional<AssetDetail> findDetail(String productId) {
        return queryDetailOrFail(() -> assetDetailPort.findDetail(productId));
    }

    private List<AssetDetail> findDetails(List<String> productIds) {
        return queryDetailOrFail(() -> assetDetailPort.findDetails(productIds));
    }

    /** 详情端口调用的统一失败口径：端口异常（DB 故障）包成 {@code IllegalStateException} 上报为该步失败（empty 是正常结果）。 */
    private <T> T queryDetailOrFail(Supplier<T> query) {
        try {
            return query.get();
        } catch (Exception e) {
            throw new IllegalStateException("资产详情查询失败: " + FailureReason.of(e), e);
        }
    }

    private static String formatDetailObservation(AssetDetail detail) {
        return "描述：%s｜成色：%s｜位置：%s｜卖家：%s｜状态：%s"
                .formatted(
                        truncate(detail.description()),
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

    private static String truncate(String value) {
        String text = orDefault(value, "无");
        return text.length() > DETAIL_DESC_MAX_CHARS ? text.substring(0, DETAIL_DESC_MAX_CHARS) + "…" : text;
    }
}
