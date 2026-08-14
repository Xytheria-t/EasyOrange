package com.cartethyia.easyorange.ai.knowledge;

import java.util.List;

/**
 * 知识库向量索引端口 — 分块写入 / 移除 / 混合召回。
 * <p>
 * 实现侧在 easyorange-application（ES 适配器，{@code easyorange.search.elasticsearch.enabled=true} 激活；
 * ES 关闭时由 MySQL LIKE 降级适配器兜底），业务侧（easyorange-ai）不感知存储细节。
 */
public interface KnowledgeIndexPort {

    /** 批量写入分块（best-effort：失败由调用方记录状态，不阻塞主链路）。 */
    void ingestChunks(List<KnowledgeChunk> chunks);

    /** 移除某文档的全部分块（文档删除时同步调用）。 */
    void removeDoc(String docId);

    /**
     * 混合召回：kNN（queryEmbedding）+ BM25（query）取 topK 候选，由调用方 Cosine 重排收口。
     * 分块 embedding 为 null（摄入时 embed 失败）时仍需返回，供纯文本匹配兜底。
     */
    List<KnowledgeChunk> search(String query, List<Float> queryEmbedding, int topK);

    /** 索引是否可用（ES 未启用时为 false，摄入侧保持 PENDING 等待重试）。 */
    boolean isAvailable();
}
