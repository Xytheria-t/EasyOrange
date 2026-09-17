package com.cartethyia.easyorange.config.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * RAG 检索健康指示器 — 报告知识库检索跑在「真实索引」还是「降级兜底」上。
 * <p>
 * 与 {@link AiHealthIndicator} 同一取舍：做<b>配置状态</b>探测而非实时连通性探测——
 * 运维关心的是「召回走哪条路」，而不是每次健康检查都发起一次真实检索。
 * <ul>
 *   <li>ES 启用 → {@code UP}（混合召回：BM25 + dense_vector kNN）</li>
 *   <li>ES 关闭 → {@code UNKNOWN}，detail 标注降级后果</li>
 * </ul>
 * 之所以不报 {@code DOWN}：降级是「功能变差」而非「功能不可用」，不应拉低整体健康；
 * 但它必须被看见 —— 降级调用计数见指标 {@code easyorange.ai.rag.degraded}，
 * 生产环境则由 {@code ProdSearchGuard} 直接拒绝以降级形态启动。
 */
@Component
public class RagHealthIndicator implements HealthIndicator {

    private final boolean esEnabled;

    public RagHealthIndicator(@Value("${easyorange.search.elasticsearch.enabled:false}") boolean esEnabled) {
        this.esEnabled = esEnabled;
    }

    @Override
    public Health health() {
        if (!esEnabled) {
            return Health.unknown()
                    .withDetail("retrieval", "mysql-like-fallback")
                    .withDetail("reason", "ES disabled — recall degrades to title/content LIKE")
                    .withDetail("measuredHitRateAt5", "10% (ES path 100%, same golden set)")
                    .build();
        }
        return Health.up()
                .withDetail("retrieval", "elasticsearch")
                .withDetail("mode", "BM25 + dense_vector kNN hybrid recall")
                .build();
    }
}
