package com.cartethyia.easyorange.ai.application.listing;

import java.util.List;

/**
 * 视觉预识别的产出 — 决策轮的图片线索。决策轮是纯文本快模型，吃不到图片，画面信息只经这一份结构化
 * 结果进循环（选择依据见 {@code doc/interview/02} §5）。
 * <p>
 * 字段刻意少而扁：它给循环当检索线索（品名 → 检索词、品类 → 召回与禁售判断），不是上架信息本身——
 * 价格与合规结论必须来自工具查证，这份线索里没有任何估价字段，预识别也就无从越权。
 *
 * @param itemName       物品名称（品牌 + 型号 + 品类；作为 product_search 的查询素材）
 * @param categoryGuess  品类猜测（一个词；用于按品类召回与禁售判断的检索词）
 * @param conditionHint  成色线索（外观磨损 / 缺件等可见状态的客观描述）
 * @param visibleDetails 画面可见细节（配件 / 颜色 / 包装等，最终生成时与观察互为补充）
 */
public record ImageClues(String itemName, String categoryGuess, String conditionHint, List<String> visibleDetails) {

    public ImageClues {
        // 视觉模型可能漏字段：null 收敛成空值，进 <image_clues> 块前由调用方剥标签
        itemName = itemName == null ? "" : itemName;
        categoryGuess = categoryGuess == null ? "" : categoryGuess;
        conditionHint = conditionHint == null ? "" : conditionHint;
        visibleDetails = visibleDetails == null ? List.of() : List.copyOf(visibleDetails);
    }
}
