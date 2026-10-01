package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.KnowledgeChunk;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeMatch;
import java.util.List;

/**
 * 知识库向量索引端口 — 分块写入 / 移除 / 多路召回融合。
 * <p>
 * 实现侧在 easyorange-application（ES 适配器，{@code easyorange.search.elasticsearch.enabled=true} 激活；
 * ES 关闭时由 MySQL LIKE 降级适配器兜底），业务侧（easyorange-ai）不感知存储细节。
 */
public interface KnowledgeIndexPort {

    /** 批量写入分块（bulk 一次往返）；写失败抛出由调用方记 FAILED 走补索引 —— 吞掉会让文档带着「没写进去」的状态标成 INDEXED。 */
    void ingestChunks(List<KnowledgeChunk> chunks);

    /** 移除一个文档的全部分块（best-effort：失败只告警，残留孤儿由重摄前的清空兜底）。 */
    void removeDoc(String docId);

    /**
     * 检索：kNN（queryEmbedding）与 BM25（query）两路召回，按 RRF 融合排名后返回 topK。
     * <p>
     * 融合必须在实现侧完成（要拿到两路的完整排名才能算融合分），返回顺序即最终顺序，调用方不再重排。
     * 两路都不可用时返回空列表；单路可用时按单路排名返回。命中不再回传分块向量。
     */
    List<KnowledgeMatch> search(String query, List<Float> queryEmbedding, int topK);

    /** 索引是否可用（ES 未启用时为 false，摄入侧保持 PENDING 等待重试）。 */
    boolean isAvailable();
}
