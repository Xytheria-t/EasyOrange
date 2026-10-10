package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import java.util.List;

/**
 * 在售资产检索端口 — 对话式找货的召回侧，与 {@code KnowledgeIndexPort} 同形态、语料换成「在售资产」。
 * 两路独立召回 + 排名融合，不合成一次「knn + query 同请求」（同请求拿不到各自排名，见实现类注释）；
 * 只读消费方，商品索引由 product 模块维护。
 */
public interface AssetRetrievalPort {

    /**
     * 两路召回并融合，返回排序后的资产命中。
     *
     * @param query 关键词（空则只跑向量那一路）
     * @param queryEmbedding 查询向量（空则只跑 BM25 那一路）
     */
    List<AssetHit> search(String query, List<Float> queryEmbedding, int topK);

    boolean isAvailable();
}
