package com.cartethyia.easyorange.ai.domain.model;

/** 知识库检索命中 — 引用溯源的最小单元（回答末尾用 [来源:标题] 标注）。score 由 {@code KnowledgeMatch} 透传，是索引侧 RRF 排名融合分，LIKE 降级路径恒为 0。 */
public record KnowledgeHit(String docId, String title, String content, double score) {}
