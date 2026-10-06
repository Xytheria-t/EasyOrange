package com.cartethyia.easyorange.ai.adapter.inbound.web.dto.response;

/**
 * 知识库检索命中视图 — RAG 检索侧的对外形状，与 {@code KnowledgeHit} 解耦。
 *
 * @param score 索引侧 RRF 排名融合分（LIKE 降级路径恒为 0，前端据此判断召回质量不可信）
 */
public record KnowledgeHitVO(String docId, String title, String content, double score) {}
