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

        List<Float> embedding = aiModelSupport.embed(embeddingModel, AiCallScope.SEMANTIC, query);
        if (embedding.isEmpty()) {
            log.warn("Empty embedding for sourcing query, falling back to BM25-only retrieval, query={}", query);
        }

        try {
            return port.search(query, embedding, topK);
        } catch (Exception e) {
            log.warn("Asset sourcing failed, chat proceeds without recommendations, query={}", query, e);
            return List.of();
        }
    }
}
