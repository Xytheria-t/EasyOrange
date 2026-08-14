package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeChunk;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeHit;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeIndexPort;
import com.cartethyia.easyorange.ai.knowledge.VectorUtils;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 知识库检索服务 — 查询向量化 → 索引侧混合召回（kNN + BM25）→ Java 原生 Cosine 重排收口。
 * <p>
 * 引用溯源：返回的 {@link KnowledgeHit} 带 docId/title，回答端据此标注 [来源:标题]。
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
     * 混合召回 + Cosine 重排，返回 topK 命中。
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
            return port.search(query, null, topK).stream()
                    .map(c -> new KnowledgeHit(c.docId(), c.title(), c.content(), 0))
                    .toList();
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

        List<KnowledgeChunk> candidates = port.search(query, queryEmbedding, topK * 2);
        return candidates.stream()
                .sorted(Comparator.comparingDouble(
                                (KnowledgeChunk c) -> VectorUtils.cosine(queryEmbedding, c.embedding()))
                        .reversed())
                .limit(topK)
                .map(c -> new KnowledgeHit(
                        c.docId(), c.title(), c.content(), VectorUtils.cosine(queryEmbedding, c.embedding())))
                .toList();
    }
}
