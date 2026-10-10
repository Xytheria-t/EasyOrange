package com.cartethyia.easyorange.ai.application.retrieval;

import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import com.cartethyia.easyorange.ai.domain.enums.AiResultCode;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.port.AssetRetrievalPort;
import com.cartethyia.easyorange.common.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 资产召回 — 对话式找货的检索侧：向量化 → kNN + BM25 两路独立召回 → RRF 融合；与
 * {@code KnowledgeRetrievalAppService} 同形态、不同语料（规则文档换成在售资产）。
 * <p>
 * <b>降级语义按调用方分层</b>：对话侧 {@link #search} 吞异常返空（少一次推荐好过整轮对话降级；抛出去后供应商
 * 抖动与代码缺陷在错误率大盘上分不开）；MCP 侧 {@link #searchStrict} 故障上抛（工具 description 承诺「空 =
 * 无在售匹配」，故障伪装成空会被外部模型误读成没货而盲目换词重试）。向量化失败两侧同语义：空向量只跑 BM25 一路。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetSourcingAppService {

    private final EmbeddingModel embeddingModel;
    private final ObjectProvider<AssetRetrievalPort> retrievalPort;
    private final AiModelSupport aiModelSupport;

    /** 对话语义：检索失败返回空列表而不抛异常（理由见类注释）。 */
    public List<AssetHit> search(String query, int topK) {
        try {
            return searchStrict(query, topK);
        } catch (Exception e) {
            log.warn("Asset sourcing failed, chat proceeds without recommendations, query={}", query, e);
            return List.of();
        }
    }

    /** MCP 语义：故障上抛经协议转错误结果。参数非法仍返回空列表（client 输入问题不是服务故障）；端口检索
     * 异常原样透出（infra 故障保留原始类型），适配器缺失抛业务码（装配问题给统一话术）。 */
    public List<AssetHit> searchStrict(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        var port = retrievalPort.getIfAvailable();
        if (port == null) {
            log.warn("Asset sourcing unavailable: ES retrieval adapter not configured");
            throw BusinessException.of(AiResultCode.AI_UNAVAILABLE, "资产检索服务不可用");
        }
        return port.search(query, embedOrEmpty(query), topK);
    }

    /** 查询向量化失败退化空向量（端口只跑 BM25 一路）—— 抛出去对话侧收成失败观察、MCP 侧变成工具报错，都比
     * 「少一路召回」糟；占位模型（key 未配置）也是调用即抛，这条路径同时承担「AI 密钥可选、不影响启动」。 */
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
