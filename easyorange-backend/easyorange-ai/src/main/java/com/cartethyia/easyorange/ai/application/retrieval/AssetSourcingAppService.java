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
 * 资产召回服务 — 对话式找货的检索侧：查询向量化 → 两路独立召回（kNN + BM25）→ RRF 排名融合 → 资产命中。
 * 与 {@code KnowledgeRetrievalAppService} 同一形态、不同语料（RAG 链路复用，检索对象从规则文档换成在售资产）。
 * <p>
 * <b>降级语义按调用方分层</b>：对话侧 {@link #search} 吞异常返空（召回为空只是少一次推荐，抛出去会让整轮对话
 * 降级，供应商抖动与代码缺陷在错误率大盘上再也分不开）；MCP 侧 {@link #searchStrict} 同一成功路径、故障上抛
 *（工具 description 向外部 client 承诺「结果为空 = 无在售匹配」，故障伪装成空结果会被误读成没货而盲目换词重试）。
 * 向量化失败两侧同语义<b>不放弃检索</b>：空向量直接传给端口只跑 BM25 一路 —— 部分降级 ≠ 不可用。
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

    /**
     * MCP 语义：故障上抛经协议转错误结果。参数非法仍返回空列表（client 输入问题不是服务故障）；
     * 端口检索异常原样透出（infra 故障保留原始类型），适配器缺失抛业务码（装配问题给统一话术）。
     */
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
