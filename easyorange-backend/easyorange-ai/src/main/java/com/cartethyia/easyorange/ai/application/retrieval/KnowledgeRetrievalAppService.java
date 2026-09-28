package com.cartethyia.easyorange.ai.application.retrieval;

import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
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
 * 排序不在这一层做：融合需要两路的完整排名，只有索引侧拿得到（见 {@link KnowledgeIndexPort}）；
 * 在这里按余弦相似度重排是对稠密路同一向量的单调变换（等于没排），还会把 BM25 的排序信号整体丢掉，不做。
 * <p>
 * 索引不可用（ES 关闭）时降级到 MySQL 标题/正文 LIKE 检索：仅保证可用，不保证召回质量。
 * <p>
 * <b>查询向量化失败同样不放弃检索</b>：空向量直接传给端口，只跑 BM25 一路 —— 与
 * {@link AssetSourcingAppService} 同一口径。两条链路都只服务对话主链路，向量化的失败方（供应商抖动 /
 * key 未配置）在资产侧会让整个找货功能静默失效，在这里也会让规则问答整条空掉；少一路召回好过没有召回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalAppService {

    private final ObjectProvider<KnowledgeIndexPort> indexPortProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final AiModelSupport aiModelSupport;

    /** 两路召回 + RRF 融合，返回 topK 命中（顺序即最终顺序）。 */
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

    /**
     * 查询向量化 —— 失败退化为空向量（端口只跑 BM25 一路），不把异常抛出去中断检索：
     * 对话侧要等工具失败被收敛成失败观察，比「整条规则问答答不出」好得多。
     * key 未配置时装配的占位模型也是调用即抛，这条降级路径同时承担「AI 密钥可选、不影响启动」的契约。
     */
    private List<Float> embedOrEmpty(EmbeddingModel embeddingModel, String query) {
        try {
            List<Float> embedding = aiModelSupport.embed(embeddingModel, AiCallScope.KNOWLEDGE, query);
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
