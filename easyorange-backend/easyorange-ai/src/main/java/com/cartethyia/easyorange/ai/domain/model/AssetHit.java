package com.cartethyia.easyorange.ai.domain.model;

import java.math.BigDecimal;

/**
 * 在售资产命中 — 对话式找货的引用溯源单元；asset 即 product 模块 {@code eo_product} 的在售商品，
 * {@code productId} 就是 {@code product.id}（AI 侧沿用「资产」表述，不是另一个实体）。与 {@link KnowledgeHit}
 * 刻意分开（一个靠 docId + 正文定性、一个靠 id + 价格定量，合成会让两边都多带用不上的字段）；productId 是
 * 防幻觉的锚点：推荐的每件资产都必须能对回一条真实记录。
 */
public record AssetHit(
        String productId, String title, BigDecimal price, String categoryName, String conditionDesc, double score) {}
