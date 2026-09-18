package com.cartethyia.easyorange.product.application.port.query;

import java.util.List;

/**
 * 检索词向量化出站端口 —— 商品检索的第二路召回（kNN）需要先把关键词编码成 dense vector。
 * <p>
 * 端口定义在 product、实现在 ai（`QueryEmbeddingAdapter`）：向量化依赖 {@code EmbeddingModel} 与
 * AI 记账设施，都在 ai 模块；方向与 {@link ProductSearchQueryPort} 由 ai 侧实现一致，
 * 不会让 product 反向依赖 ai。
 * <p>
 * <b>不可用即退化为单路召回</b>：实现方在「未配置供应商 / 调用失败」时返回空列表，
 * 调用方据此关掉 kNN 那一路，检索退化为纯 BM25 而不是整接口失败 ——
 * embedding 供应商是外部依赖，不该成为搜索可用性的前置条件。
 */
public interface QueryEmbeddingPort {

    /** 返回空列表表示当前无法向量化（未配置 / 失败），调用方必须按「单路召回」继续。 */
    List<Float> embed(String text);
}
