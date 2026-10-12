package com.cartethyia.easyorange.ai.adapter.outbound;

import com.cartethyia.easyorange.ai.application.support.ModelCallRecorder;
import com.cartethyia.easyorange.ai.application.support.ModelSupport;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.enums.ModelCallScope;
import com.cartethyia.easyorange.product.application.port.query.QueryEmbeddingPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

/**
 * 检索词向量化 — 商品搜索 kNN 那一路的编码器（实现 {@link QueryEmbeddingPort}）。
 * <p>
 * <b>对外契约是「永不抛异常」</b>：挂在商品检索主链路上、调用方不做兜底，key 缺失时装配的
 * {@code UnconfiguredEmbeddingModel} 调用即抛 —— 未配置或调用失败一律返回空列表，检索退化为纯 BM25 单路召回。
 * <p>
 * 预算走 {@code semantic} 场景：用量取供应商回报的真实 prompt token，未回报时才按场景上限估算（见 {@link ModelCallRecorder}）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryEmbeddingAdapter implements QueryEmbeddingPort {

    private final EmbeddingModel embeddingModel;
    private final ModelSupport aiModelSupport;

    @Override
    @TokenBudget(scenario = "semantic", maxTokensPerCall = 500, dailyTokenLimit = 200_000)
    public List<Float> embed(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        try {
            return aiModelSupport.embed(embeddingModel, ModelCallScope.SEMANTIC, text);
        } catch (Exception e) {
            log.warn("Query embedding unavailable, search degrades to BM25-only, text={}", text, e);
            return List.of();
        }
    }
}
