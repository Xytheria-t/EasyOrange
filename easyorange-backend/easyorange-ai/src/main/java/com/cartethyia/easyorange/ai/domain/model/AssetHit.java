package com.cartethyia.easyorange.ai.domain.model;

import java.math.BigDecimal;

/**
 * 在售资产命中 — 对话式找货的引用溯源单元。与 {@link KnowledgeHit} 刻意分开：
 * 知识库片段靠 docId + 正文定性，资产靠 id + 价格定量，合成一个 record 会让两边都拿到用不上的字段。
 * productId 是防幻觉的锚点：回答里推荐的每件资产都必须能对回一条真实记录。
 */
public record AssetHit(
        String productId, String title, BigDecimal price, String categoryName, String conditionDesc, double score) {}
