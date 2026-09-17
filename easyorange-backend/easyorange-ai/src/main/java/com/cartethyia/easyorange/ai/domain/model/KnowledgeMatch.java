package com.cartethyia.easyorange.ai.domain.model;

/**
 * 知识库检索命中（索引侧返回值）— 只带检索与引用溯源需要的字段。
 * <p>
 * 刻意不带分块向量：向量只在索引内部用于 kNN 相似度计算，检索结果回传 1024 维向量
 * 会让每条命中多背 ~10KB 的 JSON，而排序已由索引侧的多路融合决定，Java 侧不再需要它。
 *
 * @param docId      所属文档 ID（引用溯源用）
 * @param chunkIndex 块序号（0 起）
 * @param title      文档标题
 * @param content    块正文
 * @param score      融合分（RRF；纯文本降级路径恒为 0）
 */
public record KnowledgeMatch(String docId, int chunkIndex, String title, String content, double score) {}
