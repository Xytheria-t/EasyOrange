package com.cartethyia.easyorange.ai.application.service;

import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeMatch;
import com.cartethyia.easyorange.ai.domain.port.KnowledgeIndexPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 知识库检索服务 — 查询向量化 → 索引侧两路召回（kNN + BM25）+ RRF 排名融合 → 引用溯源。
 * <p>
 * 排序不在这一层做：融合需要两路的完整排名，只有索引侧拿得到（见 {@link KnowledgeIndexPort}）。
 * 此前这里按余弦相似度对候选重排，用的是与稠密召回同一个 embedding 的同一个余弦 ——
 * 对稠密那一路是单调变换（等于没排），却会把 BM25 的排序信号整体丢掉，是「看起来有重排」的假动作。
 * <p>
 * 索引不可用（ES 关闭）时降级到 MySQL 标题/正文 LIKE 检索（仅保证可用，
 * 不保证召回质量 — 面试口径：量级不到不上独立向量库，ES kNN 已够用）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private final ObjectProvider<KnowledgeIndexPort> indexPortProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final AiModelSupport aiModelSupport;

    /**
     * 两路召回 + RRF 融合，返回 topK 命中（顺序即最终顺序）。
     */
    public List<KnowledgeHit> search(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        var port = indexPortProvider.getIfAvailable();
        if (port == null) {
            return List.of();
        }
        if (!port.isAvailable()) {
            // 索引不可用（ES 关闭）— 降级适配器按标题/正文 LIKE 检索，仅保证可用不保证召回质量
            return toHits(port.search(query, null, topK));
        }
        var embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            return List.of();
        }

        List<Float> queryEmbedding;
        try {
            queryEmbedding = aiModelSupport.embed(embeddingModel, AiCallScope.KNOWLEDGE, query);
        } catch (Exception e) {
            log.warn("Query embed failed, knowledge search returns empty: {}", e.getMessage());
            return List.of();
        }

        return toHits(port.search(query, queryEmbedding, topK));
    }

    private static List<KnowledgeHit> toHits(List<KnowledgeMatch> matches) {
        return matches.stream()
                .map(m -> new KnowledgeHit(m.docId(), m.title(), m.content(), m.score()))
                .toList();
    }
}
