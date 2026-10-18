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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 多步工具循环的内部工具面 —— 模型可调用的 7 个工具（检索 3 / 计算 2 / 写入 1 / 收敛 1），{@code @Tool} 与
 * {@code @ToolParam} 注解生成供应商侧的参数 schema；方法的分节即这四类。
 * <p>
 * 在链路里的位置：由 {@code ChatToolsFactory} 每次循环造一份（{@code ChatToolLoop.run} 里造），召回物累加器是
 * 单次请求内的可变状态、由实例独占；出循环时 {@code ChatToolLoop.Result.of} 经下面的快照方法取走产物，可变引用
 * 不出实例。
 * <p>
 * 约定：thought 是每个工具的必填参数（原生 tool calling 没有独立的「决策理由」通道，只由编排器取出落 trace / SSE）；
 * 抛异常 = 该步失败，「查无此资产」这类有效结果必须返回观察文本；finish 只有 schema 没有执行；remember_preference
 * 是唯一写路径（按 userId + key 幂等 upsert），独立成一步是为了让降级路径下偏好不再静默丢失。
 */
@SuppressWarnings("unused") // thought 只进工具 schema，方法体不消费（见类注释）
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChatTools {

    // ── 工具名：@Tool(name = ...)、编排器与评测共用这一份字面量 ──

    public static final String TOOL_KNOWLEDGE_SEARCH = "knowledge_search";

    public static final String TOOL_PRODUCT_SEARCH = "product_search";

    public static final String TOOL_PRODUCT_DETAIL = "product_detail";

    public static final String TOOL_MARKET_PRICE_STATS = "market_price_stats";

    public static final String TOOL_COMPARE_ASSETS = "compare_assets";

    public static final String TOOL_REMEMBER_PREFERENCE = "remember_preference";

    public static final String TOOL_FINISH = "finish";

    /** 全集 —— {@code GoldenSetLoader} 加载期按它强校验 {@code eval/golden-set.yaml} 的 {@code expected_tools}：
     * 工具改名后标注里留着旧名会炸，而不是静默评成「路由走错」。 */
    public static final Set<String> TOOL_NAMES = Set.of(
            TOOL_KNOWLEDGE_SEARCH,
            TOOL_PRODUCT_SEARCH,
            TOOL_PRODUCT_DETAIL,
            TOOL_MARKET_PRICE_STATS,
            TOOL_COMPARE_ASSETS,
            TOOL_REMEMBER_PREFERENCE,
            TOOL_FINISH);

    // ── 取值约束与白名单 ──

    /** 偏好键白名单 — 声明序即回执文案列举序（List 保序）；与 schema 描述的同源由 ToolCallLoopTest 钉住。 */
    static final List<String> PREFERENCE_KEYS = List.of("condition", "price_range", "style", "location");

    /** 偏好值长度上限 —— value 原样落库、经 {@code <user_preferences>} 回注后续所有会话的偏好块：无上限时它是一条
     * 跨会话的存储型注入通道，上限把「塞一段话进偏好块」的成本抬到不可用。 */
    static final int PREFERENCE_VALUE_MAX_LENGTH = 60;

    /** 详情描述进观察前截断的字数 —— 描述是自由文本，长度不可控。 */
    private static final int DETAIL_DESC_MAX_CHARS = 80;

    // ── 召回物累加器：单次循环的可变状态 ──

    /** 三类检索工具的产物各一（知识 / 找货 / 详情）—— 只追加、跨步判重（检索判据在 {@link RetrievalObservations}，
     * 详情在 {@link #recordDetail}）；出循环经 {@code ChatToolLoop.Result} 定稿，供生成装配与溯源，读取只经
     * 下方快照方法，可变引用不出实例。 */
    private final List<KnowledgeHit> knowledgeHits = new ArrayList<>();

    private final List<AssetHit> assetHits = new ArrayList<>();

    private final List<AssetDetail> details = new ArrayList<>();

    // ── 协作方与归属用户 ──

    private final KnowledgeRetrievalAppService retrievalService;
    private final AssetSourcingAppService assetSourcingService;
    private final AssetDetailPort assetDetailPort;
    private final UserPreferenceRepository preferenceRepository;
    /** 偏好归属用户；机器主体（评估跑批）为 null（长期记忆不落库）。 */
    @Nullable
    private final String userId;

    // ── 累加器只读快照 ──

    List<KnowledgeHit> knowledgeHits() {
        return List.copyOf(knowledgeHits);
    }

    List<AssetHit> assetHits() {
        return List.copyOf(assetHits);
    }

    List<AssetDetail> details() {
        return List.copyOf(details);
    }

    // ── 检索类工具：知识 / 找货 / 详情 ──

    /** 查平台规则知识库，命中追加进知识累加器；查不到返回空结果观察，不是失败。 */
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

    /** 找在售资产，命中追加进资产累加器；观察文本由 {@link RetrievalObservations} 生成（含「本轮无新增」判据）。 */
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

    /** 查单件资产详情；「查无此资产」返回观察文本而非抛异常 —— 那是有效结果，让模型换目标继续。 */
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
        recordDetail(found.get());
        return formatDetailObservation(found.get());
    }

    // ── 计算类工具：零模型调用，消费累加器里的召回物 ──

    // 顺序约束写进 schema 描述而不只写 system prompt：模型是在选工具时读它的，等生成回答时才发现约束已经晚一轮
    /** 对资产累加器里的召回物算行情（件数 / 均价 / 区间），零模型调用；累加器为空即统计到空集。 */
    @Tool(
            name = TOOL_MARKET_PRICE_STATS,
            description = "对已召回的资产算行情（在售件数 / 均价 / 价格区间），用于判断某件值不值得买；零模型计算。"
                    + "必须在 product_search 之后的轮次调用（同一轮里它会排在 product_search 前面而统计到空集）",
            resultConverter = RetrievalObservations.ObservationTextConverter.class)
    public String summarizeMarketPrice(@ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought) {
        return RetrievalObservations.priceStatsObservation(assetHits);
    }

    /** 逐维比对 2-4 件候选：一次批量取详情，查不到的 ID 在观察里点出而不算失败，超 4 件截断。 */
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
                recordDetail(detail);
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

    // ── 写入类工具：唯一写路径 ──

    /** 写一条长期偏好 —— key 过白名单、值折叠空白并限长后才落库；机器主体或越界入参返回观察文本跳过，不抛异常。 */
    @Tool(
            name = TOOL_REMEMBER_PREFERENCE,
            description = "记录用户的长期偏好（成色 / 价格区间 / 风格 / 地区），跨会话生效；对话中出现明确偏好时调用一次即可，同一偏好不要重复记录",
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
            return "机器调用不落长期偏好，已跳过记录；直接继续回答即可";
        }
        String key = preferenceKey.trim();
        if (!PREFERENCE_KEYS.contains(key)) {
            // 白名单（取值与 schema 描述 / prompt yml 同源）是代码层硬校验：那两处只是对模型的指令，提示注入可让它带任意 key 进来
            return "偏好类别仅支持 %s，已跳过记录；直接继续回答即可".formatted(String.join(" / ", PREFERENCE_KEYS));
        }
        // 折叠空白（含换行）：偏好块按 \n 分行回注，换行能让一条值跨会话伪造出「额外一条偏好」
        String value = preferenceValue.trim().replaceAll("\\s+", " ");
        if (value.length() > PREFERENCE_VALUE_MAX_LENGTH) {
            return "偏好取值过长（上限 %d 字），请概括成短语再记；直接继续回答即可".formatted(PREFERENCE_VALUE_MAX_LENGTH);
        }
        try {
            preferenceRepository.record(userId, key, value);
        } catch (Exception e) {
            // DB 故障是真实故障（区别于「查无此资产」那类有效结果），按抛异常 = 该步失败上报
            throw new IllegalStateException("偏好记录失败: " + FailureReason.of(e), e);
        }
        return "已记录偏好：%s = %s".formatted(key, value);
    }

    // ── 收敛 ──

    /** 收敛信号 —— 模型调它即表示信息已够；本方法无副作用，参数只进 schema。 */
    @Tool(name = TOOL_FINISH, description = "信息已足够回答，或无需检索（寒暄 / 闲聊），不再调用任何工具")
    public String finish(@ToolParam(description = "收敛理由，不超过 20 字的中文概括") String thought) {
        // 方法体永不执行，这里只为让 finish 出现在发给供应商的工具 schema 里
        return TOOL_FINISH;
    }

    // ── 内部方法（不进工具 schema） ──

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

    /** 详情累加器判重 — 与 hits 同口径（保留首次出现）：重复详情会让生成侧 {@code <asset_details>} 块重复同一资产。 */
    private void recordDetail(AssetDetail detail) {
        boolean alreadyRecorded =
                details.stream().anyMatch(existing -> Objects.equals(existing.productId(), detail.productId()));
        if (!alreadyRecorded) {
            details.add(detail);
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
