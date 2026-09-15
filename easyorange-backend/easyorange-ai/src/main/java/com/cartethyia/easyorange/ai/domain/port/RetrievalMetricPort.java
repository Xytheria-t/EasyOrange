package com.cartethyia.easyorange.ai.domain.port;

/**
 * 检索指标端口 — 每条金标准检索用例落一行，按 runId 聚合产出 hit@5 / MRR 趋势。
 * <p>
 * 实现方在 adapter/outbound（{@code eo_retrieval_metric} 表）；记录失败只告警不抛出。
 */
public interface RetrievalMetricPort {

    void record(String runId, String caseId, String query, String goldDocIds, boolean hit, double reciprocalRank);
}
