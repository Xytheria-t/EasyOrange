package com.cartethyia.easyorange.ai.domain.port;

import org.jspecify.annotations.Nullable;

/**
 * AI 调用日志端口 — 每次 LLM/Embedding 调用落一条记录，作为**成本报表**的数据源。
 * <p>
 * 实现方在 adapter/outbound（{@code eo_ai_call_log} 表）；记录失败只告警不抛出，
 * 调用日志是观测副产物，绝不能影响主链路。
 * <p>
 * 用量是**成本可见性**的地基：没有 token 列就只能出「谁调用得多」，出不了「谁花得多」。
 * 供应商未回报用量时记 0（不估算）—— 估算值混进成本报表比缺数据更危险，估算口径留给预算器。
 */
public interface AiCallLogPort {

    void record(
            String scope,
            String model,
            String promptHash,
            String response,
            long latencyMs,
            int tokenInput,
            int tokenOutput,
            boolean success,
            @Nullable String errorMsg);
}
