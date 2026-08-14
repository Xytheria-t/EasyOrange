package com.cartethyia.easyorange.ai.knowledge;

/**
 * 知识库检索命中 — 引用溯源的最小单元（回答末尾用 [来源:标题] 标注）。
 *
 * @param docId   命中文档 ID
 * @param title   文档标题
 * @param content 命中的分块正文
 * @param score   与查询的余弦相似度（重排后的分数）
 */
public record KnowledgeHit(String docId, String title, String content, double score) {}
