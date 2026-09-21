package com.cartethyia.easyorange.ai.application.service;

import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
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
 * <p>
 * 与 {@code KnowledgeRetrievalService} 是同一形态、不同语料：RAG 链路（两路独立召回 / {@code RrfFusion}
 * 排名融合 / 引用溯源 / 评测口径）一行没改，换的只是检索对象 —— 从「平台规则文档」换成「在售资产」，
 * 引用溯源就从客服问答挪到了找货这条交易主链路上。
 * <p>
 * 向量化失败<b>不放弃检索</b>：空向量直接传给端口，让它只跑 BM25 那一路 —— 少一路召回好过整个找货功能
 * 因 embedding 供应商抖动而静默失效（与知识库侧「单路失败退化为单路召回」同一取向）。
 * <p>
 * 检索失败返回空列表而不抛异常：调用方是对话主链路，召回为空只是少一次推荐，抛出去会让整轮对话降级
 * —— 供应商抖动与代码缺陷在错误率大盘上就再也分不开了。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetSourcingService {

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
     * 查询向量化 —— 失败退化为空向量，而不是把异常抛出去中断检索：空向量交给端口只跑 BM25 那一路
     * （见 {@link AssetRetrievalPort#search} 的入参约定）。抛出去在这条链路上没有收益：对话侧要等
     * 工具失败被收敛成失败观察、MCP 侧直接变成工具报错，两处都比「少一路召回」更糟。
     * <p>
     * 抛异常的不只是供应商抖动 —— key 未配置时装配的占位模型（{@code UnconfiguredEmbeddingModel}）
     * 也是调用即抛，这条降级路径同时承担「AI 密钥可选、不影响应用启动」的契约。
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
