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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.execution.ToolCallResultConverter;

/**
 * Agent 循环的内部工具面 — 7 个工具的 schema 与执行都在这里：{@code @Tool} / {@code @ToolParam} 注解
 * 生成供应商侧校验的 JSON Schema，方法体即「执行 + 观察格式化」。
 * <p>
 * 每次循环实例化一份：召回物累加器是单次请求内的可变状态（跨轮累加，供最终生成做引用溯源），
 * 换一次请求就换一个实例。框架不执行这些工具 —— Spring AI 2.0 的 {@code ChatModel.call} 只把 tool
 * 定义发给供应商、原样返回 tool call，执行与循环控制权都在 {@link AgentLoopRunner}
 * （步数上限 / 预算 / 降级在那边，这里只管单个工具的语义）。
 * <p>
 * 四点非显而易见的约定：
 * <ul>
 *   <li><b>thought 是每个工具的必填参数</b> —— 原生 tool calling 没有独立的「决策理由」通道，理由只能
 *       随参数带回；工具方法不消费它，由 runner 取出落 trace / 推 SSE。</li>
 *   <li><b>抛异常 = 该步失败</b> —— runner 把异常收敛成失败观察交回模型（带错误反馈的修复轮），
 *       所以「查无此资产」这类<b>有效</b>结果必须返回观察文本而不是抛异常。</li>
 *   <li><b>finish 只有 schema 没有执行</b> —— 收敛轮由 runner 在执行前按名称拦截，方法体不会被调用。</li>
 *   <li><b>remember_preference 是唯一的写路径</b> —— 长期画像 upsert，按 (userId, key) 唯一键幂等。
 *       它从 finish 的参数副作用提升为独立工具，是为了让「写入长期记忆」成为模型自主决策的一步，
 *       并且不再依赖收敛成功：原先只在 finish 轮提取，步数超限 / 预算耗尽 / 决策失败三条降级路径下
 *       偏好会静默丢失。</li>
 * </ul>
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

    /** 观察里列举的命中条数上限 —— 观察是给下一轮决策的摘要，召回多少条都只列举这么多。 */
    private static final int OBSERVATION_SUMMARY_LIMIT = 3;

    /**
     * 检索无新增时的观察文案 —— 收敛判据本身（见 {@link #knowledgeSearch} / {@link #productSearch}）。
     * <p>
     * 判据是「本轮命中的条目有多少此前已经出现过」：换关键词检索回来还是同一批文档，对回答没有增量，
     * 而模型不会自己看出这一点（每次观察都写着「命中 N 条」，看上去总有进展）。把这件事作为一条明确
     * 观察交回模型，比在循环里硬性拦掉这次调用更合适 —— 拦掉只是少一轮反馈，让模型自己看到
     * 「再查也是重复」才是它该学到的判断。
     */
    private static final String NO_NEW_HIT_OBSERVATION = "本次检索无新增信息（命中的内容此前已出现过）：请直接调用 finish 基于已有信息作答，不要再换关键词重试";

    /**
     * 判为「无新增」的重合比例阈值 —— 本轮命中里此前出现过的条目占比达到此值即收敛。
     * <p>
     * 只认「完全重复」不够用：检索是 topK 截断的，换个关键词常返回与上次高度重叠但不完全相同的一批
     * （每轮夹带一两个新条目），逐条判重会让模型无限换词直到撞步数上限 —— 实测正是如此，7 步里 5 步是
     * 换词重搜。阈值取 0.6：首次检索重合率为 0，而一次检索能带进 3 条以上新内容时（约 40% 重合）不算冗余。
     */
    private static final double REDUNDANT_OVERLAP_RATIO = 0.6;

    /** 详情描述进观察前截断的字数 —— 描述是自由文本，长度不可控。 */
    private static final int DETAIL_DESC_MAX_CHARS = 80;

    private final List<KnowledgeHit> knowledgeHits;
    private final List<AssetHit> assets;
    private final List<AssetDetail> details;
    private final KnowledgeRetrievalService retrievalService;
    private final AssetSourcingService assetSourcingService;
    private final AssetDetailPort assetDetailPort;
    private final UserPreferenceRepository preferenceRepository;
    /** 画像归属用户；匿名会话为 null（长期记忆不落库）。 */
    private final String userId;

    AgentTools(
            List<KnowledgeHit> knowledgeHits,
            List<AssetHit> assets,
            List<AssetDetail> details,
            KnowledgeRetrievalService retrievalService,
            AssetSourcingService assetSourcingService,
            AssetDetailPort assetDetailPort,
            UserPreferenceRepository preferenceRepository,
            String userId) {
        this.knowledgeHits = knowledgeHits;
        this.assets = assets;
        this.details = details;
        this.retrievalService = retrievalService;
        this.assetSourcingService = assetSourcingService;
        this.assetDetailPort = assetDetailPort;
        this.preferenceRepository = preferenceRepository;
        this.userId = userId;
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
        for (String id : targets) {
            Optional<AssetDetail> detail = findDetail(id);
            if (detail.isPresent()) {
                found.add(detail.get());
                details.add(detail.get());
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
            return "匿名会话不落长期画像，已跳过记录；直接继续回答即可";
        }
        String key = preferenceKey.trim();
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
        // 方法体不会被执行：收敛轮由 runner 在执行前按名称拦截（finish 不产生 observation 与耗时），
        // 这里的存在意义是让 finish 出现在发给供应商的工具 schema 里
        return TOOL_FINISH;
    }

    /**
     * 按 ID 查资产详情 — product_detail 与 compare_assets 共用同一条通道与同一种失败语义：
     * 端口抛出（DB 故障）按工具失败上报（模型可换目标重试），empty（查无此资产）是正常结果。
     */
    private Optional<AssetDetail> findDetail(String productId) {
        try {
            return assetDetailPort.findDetail(productId);
        } catch (Exception e) {
            throw new IllegalStateException("资产详情查询失败: " + reasonOf(e), e);
        }
    }

    /**
     * 本轮检索是否已无新增信息 —— 「此前出现过的条目数 / 本轮命中数」达到阈值即判冗余。
     * 完全没召回到（{@code foundCount == 0}）不算冗余：那是「换个关键词还能试」的空结果，不是重复。
     */
    private static boolean isRedundant(int seenCount, int foundCount) {
        return foundCount > 0 && (double) seenCount / foundCount >= REDUNDANT_OVERLAP_RATIO;
    }

    /**
     * 保留本轮新增的文档 —— 已在累加器里的（此前轮次召回过）不再重复计入，判据取 docId，
     * 缺失时退回标题（ES 命中必有 docId，兜底只为 LIKE 降级路径不因 null 误判成「全新增」）。
     */
    private List<KnowledgeHit> retainNewKnowledge(List<KnowledgeHit> found) {
        Set<String> seen = knowledgeHits.stream().map(AgentTools::knowledgeKey).collect(Collectors.toSet());
        return found.stream().filter(hit -> seen.add(knowledgeKey(hit))).collect(Collectors.toList());
    }

    /** 同 {@link #retainNewKnowledge}，资产按 productId 判重。 */
    private List<AssetHit> retainNewAssets(List<AssetHit> found) {
        Set<String> seen = assets.stream().map(asset -> assetKey(asset)).collect(Collectors.toSet());
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

    /**
     * 观察文本原样返回 — 默认的 {@code DefaultToolCallResultConverter} 会把返回值 JSON 序列化，
     * String 结果因此多一层引号（观察变成 {@code "命中 1 条：…"}）；本工具面的观察是进下一轮 prompt
     * 的纯文本，不需要引号。finish 无执行体、不需要该转换器。
     */
    @NullMarked
    public static final class ObservationTextConverter implements ToolCallResultConverter {

        @Override
        public String convert(@Nullable Object result, @Nullable Type returnType) {
            return result == null ? "" : String.valueOf(result);
        }
    }
}
