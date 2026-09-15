package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import java.util.Optional;

/**
 * 语义缓存端口 — 按 scope + 查询文本缓存 AI 响应，命中即跳过模型调用。
 * <p>
 * 实现方在 adapter/outbound（向量相似度匹配）；未命中返回 {@link Optional#empty()}。
 */
public interface SemanticCachePort {

    <T> Optional<T> get(AiCallScope scope, String query, Class<T> type);

    void put(AiCallScope scope, String query, Object response);
}
