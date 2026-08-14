package com.cartethyia.easyorange.ai.knowledge;

import java.util.List;

/**
 * 知识库分块 — 摄入管线产物（解析 → 分块 → embed 后交给索引侧）。
 *
 * @param docId     所属文档 ID
 * @param chunkIndex 块序号（0 起）
 * @param title     文档标题（检索展示与引用溯源用）
 * @param content   块正文
 * @param embedding 块向量（1024 维，embedding 失败时可为 null，检索侧降级纯文本匹配）
 */
public record KnowledgeChunk(String docId, int chunkIndex, String title, String content, List<Float> embedding) {}
