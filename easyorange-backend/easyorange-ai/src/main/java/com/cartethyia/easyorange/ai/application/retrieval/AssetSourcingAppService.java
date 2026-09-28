package com.cartethyia.easyorange.ai.application.retrieval;

import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.port.AssetRetrievalPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 资产召回服务 — 对话式找货的检索侧：查询向量化 → 两路独立召回（kNN + BM25）→ RRF 排名融合 → 资产命中。
 * 与 {@code KnowledgeRetrievalAppService} 同一形态、不同语料（RAG 链路复用，检索对象从规则文档换成在售资产）。
 * <p>
 * 向量化失败<b>不放弃检索</b>：空向量直接传给端口，只跑 BM25 一路 —— 少一路召回好过整个找货功能因
 * embedding 供应商抖动而静默失效。检索失败返回空列表而不抛异常：调用方是对话主链路，召回为空只是少
 * 一次推荐，抛出去会让整轮对话降级，供应商抖动与代码缺陷在错误率大盘上就再也分不开。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetSourcingAppService {

    private final EmbeddingModel embeddingModel;
    private final ObjectProvider<AssetRetrievalPort> retrievalPort;
    private final AiModelSupport aiModelSupport;

    public List<AssetHit> search(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        var port = retrievalPort.getIfAvailable();
        if (port == null) {
            log.warn("Asset sourcing unavailable: ES retrieval adapter not configured");
            return List.of();
        }

        List<Float> embedding = embedOrEmpty(query);

        try {
            return port.search(query, embedding, topK);
        } catch (Exception e) {
            log.warn("Asset sourcing failed, chat proceeds without recommendations, query={}", query, e);
            return List.of();
        }
    }

    /**
     * 查询向量化 —— 失败退化为空向量（端口只跑 BM25 一路），不把异常抛出去中断检索：
     * 对话侧要等工具失败被收敛成失败观察、MCP 侧直接变成工具报错，都比「少一路召回」糟。
     * key 未配置时装配的占位模型也是调用即抛，这条降级路径同时承担「AI 密钥可选、不影响启动」的契约。
     */
    private List<Float> embedOrEmpty(String query) {
        try {
            List<Float> embedding = aiModelSupport.embed(embeddingModel, AiCallScope.SEMANTIC, query);
            if (embedding.isEmpty()) {
                log.warn("Empty embedding for sourcing query, falling back to BM25-only retrieval, query={}", query);
            }
            return embedding;
        } catch (Exception e) {
            log.warn("Query embed failed, sourcing falls back to BM25-only retrieval, query={}", query, e);
            return List.of();
        }
    }
}
