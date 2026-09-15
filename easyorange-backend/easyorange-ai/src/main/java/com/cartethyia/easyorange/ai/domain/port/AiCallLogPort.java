package com.cartethyia.easyorange.ai.domain.port;

/**
 * AI 调用日志端口 — 每次 LLM/Embedding 调用落一条记录，作为 LLM-as-Judge 离线评估的数据源。
 * <p>
 * 实现方在 adapter/outbound（{@code eo_ai_call_log} 表）；记录失败只告警不抛出，
 * 调用日志是观测副产物，绝不能影响主链路。
 */
public interface AiCallLogPort {

    void record(
            String scope,
            String model,
            String promptHash,
            String response,
            long latencyMs,
            boolean success,
            String errorMsg);
}
