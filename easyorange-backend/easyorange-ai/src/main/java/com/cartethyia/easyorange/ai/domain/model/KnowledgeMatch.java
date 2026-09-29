package com.cartethyia.easyorange.ai.domain.model;

/** 知识库检索命中（索引侧返回值）— 刻意不带分块向量：排序已由索引侧多路融合决定，回传 1024 维每条要多背约 10KB JSON；score 是 RRF 融合分，纯文本降级路径恒为 0，chunkIndex 从 0 起。 */
public record KnowledgeMatch(String docId, int chunkIndex, String title, String content, double score) {}
