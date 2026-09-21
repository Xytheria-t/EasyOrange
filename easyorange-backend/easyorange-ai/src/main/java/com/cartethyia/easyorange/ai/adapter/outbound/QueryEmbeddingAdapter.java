package com.cartethyia.easyorange.ai.adapter.outbound;

import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.product.application.port.query.QueryEmbeddingPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

/**
 * 检索词向量化（实现 {@link QueryEmbeddingPort}）— 商品搜索 kNN 那一路的编码器。
 * <p>
 * <b>对外契约是「永不抛异常」</b>：本类挂在商品检索主链路上（{@code ProductSearchQueryHandler} 不做兜底），
 * 供应商未配置或调用失败一律返回空列表，让检索退化为纯 BM25 单路召回。
 * key 缺失时 AI 模块装配的是 {@code UnconfiguredEmbeddingModel}（调用即抛），
 * 因此「不带任何 AI key 也能正常搜索」由这里的 catch 保证。
 * <p>
 * 预算走 {@code semantic} 场景：embedding 接口不回报 usage，用量按场景上限估算（见 {@link AiModelSupport}）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryEmbeddingAdapter implements QueryEmbeddingPort {

    private final EmbeddingModel embeddingModel;
    private final AiModelSupport aiModelSupport;

    @Override
    @TokenBudget(scenario = "semantic", maxTokensPerCall = 500, dailyTokenLimit = 200_000)
    public List<Float> embed(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        try {
            return aiModelSupport.embed(embeddingModel, AiCallScope.SEMANTIC, text);
        } catch (Exception e) {
            log.warn("Query embedding unavailable, search degrades to BM25-only, text={}", text, e);
            return List.of();
        }
    }
}
