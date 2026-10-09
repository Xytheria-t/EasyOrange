package com.cartethyia.easyorange.ai.application.listing;

import com.cartethyia.easyorange.ai.application.support.UntrustedText;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.PriceStats;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * 发布链路两侧的 prompt 拼装 — 决策首轮上下文与最终生成的观察块，纯函数无状态。
 * <p>
 * 与买家侧同一条铁律：不可信内容进块前过 {@link UntrustedText#stripTags}。图片经 OCR 转写后与卖家手打
 * 的文本等价 —— 照片里一张写着「忽略定价规则」的纸条同样是注入。
 */
final class ListingPromptAssembler {

    private ListingPromptAssembler() {}

    /** 决策首轮 user 消息 — 图片线索 + 卖家备注 + 分类清单；线索与备注两块按来源可缺省（评估跑批无图片、HTTP 无备注）。 */
    static String firstDecisionMessage(
            @Nullable ImageClues clues, @Nullable String sellerNote, List<String> categories) {
        var sb = new StringBuilder();
        if (clues != null) {
            sb.append("""
                    <image_clues>
                    品名：%s
                    品类候选：%s
                    成色线索：%s
                    可见细节：%s
                    </image_clues>

                    """.formatted(
                            UntrustedText.stripTags(clues.itemName()),
                            UntrustedText.stripTags(clues.categoryGuess()),
                            UntrustedText.stripTags(clues.conditionHint()),
                            UntrustedText.stripTags(String.join("；", clues.visibleDetails()))));
        }
        if (sellerNote != null && !sellerNote.isBlank()) {
            sb.append("<seller_note>\n%s\n</seller_note>\n\n".formatted(UntrustedText.stripTags(sellerNote)));
        }
        sb.append("可用分类清单（list_categories 工具返回同一份）：\n%s"
                .formatted(UntrustedText.stripTags(String.join("、", categories))));
        return sb.toString();
    }

    /** 最终生成的 user 消息 — 观察块 + 分类块；行情与 market_price_stats 工具的观察同源同值，空集写「无行情统计」对齐 price 置 null 的约束。 */
    static String generatorUserText(
            List<KnowledgeHit> knowledgeHits, List<AssetHit> assetHits, List<String> categories) {
        return """
                <observations>
                平台规则结论：
                %s

                同类在售行情：
                %s
                </observations>

                <category_options>
                %s
                </category_options>
                """.formatted(
                        UntrustedText.stripTags(formatKnowledgeHits(knowledgeHits)),
                        UntrustedText.stripTags(priceStatsText(assetHits)),
                        UntrustedText.stripTags(String.join("、", categories)));
    }

    private static String formatKnowledgeHits(List<KnowledgeHit> hits) {
        if (hits.isEmpty()) {
            return "(无检索结果)";
        }
        var sb = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            KnowledgeHit hit = hits.get(i);
            sb.append("[%d] (%s)\n%s\n".formatted(i + 1, hit.title(), hit.content()));
        }
        return sb.toString();
    }

    private static String priceStatsText(List<AssetHit> assetHits) {
        return PriceStats.of(assetHits).map(PriceStats::observation).orElse("(无行情统计)");
    }
}
