package com.cartethyia.easyorange.ai.domain.model;

import java.util.List;

/**
 * 向量相似度工具 — 两个等长向量的余弦相似度。
 * <p>
 * 仅用于**语义回答缓存**（`SemanticCacheService` 在 Redis Hash 里按余弦找最相似的已缓存问题）。
 * 检索排序不走这里：知识库检索是 kNN + BM25 两路召回后在索引侧做 RRF 排名融合，
 * 余弦与 BM25 分值量纲不可比，回到 Java 侧按余弦重排会把 BM25 的排序信号整体丢掉（ADR-0012）。
 */
public final class VectorUtils {

    private VectorUtils() {}

    /**
     * 余弦相似度；任一向量为空/长度不一致返回 0（空向量不参与排序）。
     */
    public static double cosine(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty() || a.size() != b.size()) {
            return 0;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.size(); i++) {
            double x = a.get(i);
            double y = b.get(i);
            dot += x * y;
            normA += x * x;
            normB += y * y;
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
