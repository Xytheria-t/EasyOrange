package com.cartethyia.easyorange.ai.domain.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal Rank Fusion — 多路召回结果的排名融合（Cormack et al. 2009）。
 * <p>
 * 融合分 {@code score(d) = Σ_r 1 / (k + rank_r(d))}，{@code rank} 从 1 起，{@code k} 默认 60。
 * <b>只用排名、不用分数</b>：kNN 的余弦相似度与 BM25 的相关性分量纲不可比，
 * 直接加权求和要调参且对分布漂移敏感；排名是对量纲无关的稳定信号。
 * <p>
 * 单路召回的排序问题（例如只用同一 embedding 的余弦对稠密候选重排，等价于单调变换、等于没排）
 * 不是融合能解决的 —— 那也是为什么这里必须有两条独立召回路才有意义。
 */
public final class RrfFusion {

    /** 平滑常数：取 60 是原论文的经验值，压制「第 1 名 vs 第 2 名」的过度优势。 */
    public static final int DEFAULT_K = 60;

    private RrfFusion() {}

    /** 融合后的单条命中：文档 ID + 融合分。 */
    public record Fused(String id, double score) {}

    /**
     * 融合多路有序 ID 列表（每路按相关性降序）并返回按融合分降序的结果。
     * <p>
     * 同分保持「首次出现的路序 + 路内先后」—— 依赖 {@link LinkedHashMap} 的插入序与
     * 稳定排序，让同一批输入每次得到同一顺序（评测可复现）。
     */
    public static List<Fused> fuse(int k, List<List<String>> rankedLists) {
        Map<String, Double> scores = new LinkedHashMap<>();
        for (List<String> ranked : rankedLists) {
            for (int i = 0; i < ranked.size(); i++) {
                String id = ranked.get(i);
                scores.merge(id, 1.0 / (k + i + 1), Double::sum);
            }
        }
        List<Map.Entry<String, Double>> entries = new ArrayList<>(scores.entrySet());
        entries.sort(Map.Entry.<String, Double>comparingByValue().reversed());
        return entries.stream().map(e -> new Fused(e.getKey(), e.getValue())).toList();
    }
}
