package com.cartethyia.easyorange.ai.application.retrieval;

import com.cartethyia.easyorange.ai.application.support.ModelSupport;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.enums.ModelCallScope;
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
 * 知识库检索服务 — 查询向量化 → 索引侧两路召回（kNN + BM25）+ RRF 融合 → 引用溯源。
 * 排序不在这一层做：融合要两路的完整排名（只有索引侧拿得到，见 {@link KnowledgeIndexPort}）；按余弦相似度
 * 重排是对稠密路同一向量的单调变换（等于没排），还会丢掉 BM25 的排序信号，不做。
 * <p>
 * 索引不可用（ES 关闭）时降级 MySQL 标题/正文 LIKE：只保证可用，不保证召回质量。查询向量化失败同样不
 * 放弃检索：空向量直接传端口只跑 BM25 一路，与 {@link AssetSourcingAppService} 同口径 —— 两条链路都只
 * 服务对话主链路，少一路召回好过整条规则问答空掉。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalAppService {

    private final ObjectProvider<KnowledgeIndexPort> indexPortProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final ModelSupport aiModelSupport;

    /** 两路召回 + RRF 融合，返回 topK 命中（顺序即最终顺序）；预算闸门挂这一层 —— 向量化是本场景唯一计费
     * 点，匿名 MCP 知识检索也走此路，没有它外部 client 换个 IP 就能无限触发供应商 embedding 计费
     * （限流按 IP 计数，不是成本闸门）。 */
    @TokenBudget(scenario = "knowledge", maxTokensPerCall = 500, dailyTokenLimit = 200_000)
    public List<KnowledgeHit> search(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        var port = indexPortProvider.getIfAvailable();
        if (port == null) {
            return List.of();
        }
        if (!port.isAvailable()) {
            return toHits(port.search(query, null, topK));
        }
        var embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            return List.of();
        }

        return toHits(port.search(query, embedOrEmpty(embeddingModel, query), topK));
    }

    /** 向量化失败退化空向量（端口只跑 BM25 一路）—— 抛出去会让整条规则问答答不出；占位模型（key 未配置）
     * 也是调用即抛，这条路径同时承担「AI 密钥可选、不影响启动」的契约。 */
    private List<Float> embedOrEmpty(EmbeddingModel embeddingModel, String query) {
        try {
            List<Float> embedding = aiModelSupport.embed(embeddingModel, ModelCallScope.KNOWLEDGE, query);
            if (embedding.isEmpty()) {
                log.warn("Empty embedding for knowledge query, falling back to BM25-only retrieval, query={}", query);
            }
            return embedding;
        } catch (Exception e) {
            log.warn("Query embed failed, knowledge search falls back to BM25-only retrieval, query={}", query, e);
            return List.of();
        }
    }

    private static List<KnowledgeHit> toHits(List<KnowledgeMatch> matches) {
        return matches.stream()
                .map(m -> new KnowledgeHit(m.docId(), m.title(), m.content(), m.score()))
                .toList();
    }
}
