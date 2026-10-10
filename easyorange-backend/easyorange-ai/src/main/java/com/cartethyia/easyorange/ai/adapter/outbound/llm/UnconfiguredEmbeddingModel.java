package com.cartethyia.easyorange.ai.adapter.outbound.llm;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * AI 未配置时的占位 {@link EmbeddingModel} — embedding key 缺失时装配。
 * <p>
 * 调用即抛「AI 模型未配置」异常，由服务层现有 try/catch 降级（fail-open）：
 * 保证无需任何 AI key 应用也能启动。
 */
public class UnconfiguredEmbeddingModel implements EmbeddingModel {

    private final String reason;

    public UnconfiguredEmbeddingModel(String reason) {
        this.reason = reason;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        throw new IllegalStateException("AI 模型未配置：" + reason);
    }

    @Override
    public float[] embed(Document document) {
        throw new IllegalStateException("AI 模型未配置：" + reason);
    }
}
