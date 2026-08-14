package com.cartethyia.easyorange.ai.knowledge;

import java.util.List;

/**
 * 向量相似度工具 — 查询向量与候选向量的余弦相似度（Java 原生重排收口）。
 * <p>
 * ES kNN 已按 cosine 打分，但混合召回（kNN + BM25）后排序口径不一，
 * 统一回到 Java 侧用余弦重排收口，避免依赖 ES 侧两种打分不可比。
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
