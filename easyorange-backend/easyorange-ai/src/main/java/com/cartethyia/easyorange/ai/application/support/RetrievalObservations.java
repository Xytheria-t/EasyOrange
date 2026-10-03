package com.cartethyia.easyorange.ai.application.support;

import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.PriceStats;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.execution.ToolCallResultConverter;

/**
 * 检索类工具的观察文本、判重与冗余收敛 — chat 与 listing 两条工具面的单一来源：判据漂移一处生效（或
 * 一处暴露），观察文案两链路逐字一致（STEP 文案与评测断言都引用它们）。
 * <p>
 * 判重口径：保留本轮新增的召回物 —— 判据取 docId（资产按 productId），缺失时退回标题，兜底只为 LIKE
 * 降级路径不因 null 误判成「全新增」。收敛判据见 {@link #isRedundant}：完全没召回到不算冗余，那是空
 * 结果不是重复。
 */
public final class RetrievalObservations {

    /** 检索 topK —— 两条工具面共用同一取值（观察摘要与检索量同量级是收敛判据的隐含前提）。 */
    public static final int TOP_K = 5;

    /** 检索无新增时的观察文案 —— 模型不会自己看出「再查也是重复」，把这件事作为一条明确观察交回比在循环里硬性拦掉更合适。 */
    public static final String NO_NEW_HIT_OBSERVATION = "本次检索无新增信息（命中的内容此前已出现过）：请直接调用 finish 基于已有信息作答，不要再换关键词重试";

    /** 观察里列举的命中条数上限 —— 观察是给下一轮决策的摘要，召回多少条都只列举这么多。 */
    private static final int OBSERVATION_SUMMARY_LIMIT = 3;

    private RetrievalObservations() {}

    /**
     * 判为「无新增」的重合比例阈值 —— 本轮命中里此前出现过的条目占比达到此值即收敛。只认「完全重复」不够：检索是 topK
     * 截断的，换关键词常返回高度重叠但不完全相同的一批，逐条判重会让模型无限换词直到撞步数上限；取 0.6 是因为一次检索
     * 能带进 3 条以上新内容时（约 40% 重合）不算冗余。
     */
    public static boolean isRedundant(int alreadySeenCount, int foundCount) {
        return foundCount > 0 && (double) alreadySeenCount / foundCount >= 0.6;
    }

    public static List<KnowledgeHit> freshKnowledge(List<KnowledgeHit> accumulated, List<KnowledgeHit> found) {
        Set<String> seen =
                accumulated.stream().map(RetrievalObservations::knowledgeHitId).collect(Collectors.toSet());
        return found.stream().filter(hit -> seen.add(knowledgeHitId(hit))).collect(Collectors.toList());
    }

    public static List<AssetHit> freshAssets(List<AssetHit> accumulated, List<AssetHit> found) {
        Set<String> seen =
                accumulated.stream().map(RetrievalObservations::assetHitId).collect(Collectors.toSet());
        return found.stream().filter(asset -> seen.add(assetHitId(asset))).collect(Collectors.toList());
    }

    /** 一次检索的工具产物 — fresh 追加进累加器、observation 回给模型；fresh 在判冗余前算好，两步共用一次判定。 */
    public record Turn<T>(List<T> fresh, String observation) {}

    public static Turn<KnowledgeHit> knowledgeTurn(List<KnowledgeHit> accumulated, List<KnowledgeHit> found) {
        List<KnowledgeHit> fresh = freshKnowledge(accumulated, found);
        String observation = isRedundant(found.size() - fresh.size(), found.size())
                ? NO_NEW_HIT_OBSERVATION
                : knowledgeObservation(found);
        return new Turn<>(fresh, observation);
    }

    public static Turn<AssetHit> assetTurn(List<AssetHit> accumulated, List<AssetHit> found) {
        List<AssetHit> fresh = freshAssets(accumulated, found);
        String observation = isRedundant(found.size() - fresh.size(), found.size())
                ? NO_NEW_HIT_OBSERVATION
                : assetObservation(found);
        return new Turn<>(fresh, observation);
    }

    public static String knowledgeObservation(List<KnowledgeHit> found) {
        if (found.isEmpty()) {
            return "知识库未命中，可换关键词重试或直接 finish";
        }
        String titles = found.stream()
                .limit(OBSERVATION_SUMMARY_LIMIT)
                .map(KnowledgeHit::title)
                .collect(Collectors.joining(" / "));
        return "命中 %d 条：%s".formatted(found.size(), titles);
    }

    public static String assetObservation(List<AssetHit> found) {
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

    /** 行情观察 — 零模型计算，空集提示指向 product_search（两条链路该工具同名同义）。 */
    public static String priceStatsObservation(List<AssetHit> hits) {
        return PriceStats.of(hits)
                .map(PriceStats::observation)
                .orElse("暂无可统计的在售资产（尚未召回，或召回项均无有效价格），先调用 product_search 召回候选");
    }

    private static String knowledgeHitId(KnowledgeHit hit) {
        return isBlank(hit.docId()) ? String.valueOf(hit.title()) : hit.docId();
    }

    private static String assetHitId(AssetHit asset) {
        return isBlank(asset.productId()) ? String.valueOf(asset.title()) : asset.productId();
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    /** 观察文本原样返回 — 默认转换器会把 String 返回值 JSON 序列化（观察多一层引号），观察是进下一轮 prompt 的纯文本。finish 无执行体、不需要该转换器。 */
    @NullMarked
    public static final class ObservationTextConverter implements ToolCallResultConverter {

        @Override
        public String convert(@Nullable Object result, @Nullable Type returnType) {
            return result == null ? "" : String.valueOf(result);
        }
    }
}
