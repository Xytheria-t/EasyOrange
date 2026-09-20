package com.cartethyia.easyorange.product.domain.valueobject;

import java.math.BigDecimal;

/**
 * AI 建议快照 — 拍照识别给出的六个字段原样留档。
 * <p>
 * 它不是业务状态：不参与定价、不参与状态流转，唯一用途是让「AI 建议的字段有没有被资产方改掉」
 * 可被按字段查询 —— 全项目唯一不依赖 LLM 评 LLM 的质量数字由此得来。
 * <p>
 * 刻意保留**未加工的原文**而不是预先算好的采纳结论：口径以后要收紧（例如文本字段改成相似度阈值）
 * 时，历史数据可以直接按新口径重算，不需要回填。
 */
public record AiSuggestion(
        String title,
        String description,
        BigDecimal price,
        String categoryName,
        String conditionLevel,
        String location) {}
