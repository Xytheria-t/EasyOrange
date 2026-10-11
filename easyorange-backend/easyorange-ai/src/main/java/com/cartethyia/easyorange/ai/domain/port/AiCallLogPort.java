package com.cartethyia.easyorange.ai.domain.port;

import org.jspecify.annotations.Nullable;

/**
 * AI 调用日志端口 — 每次 LLM/Embedding 调用落一条记录，是<b>成本报表</b>的数据源（表
 * {@code eo_ai_call_log}，实现在 adapter/outbound）；记录失败只告警不抛出，观测副产物绝不能影响主链路。
 * <p>
 * 供应商未回报用量时记 0 不估算 —— 估算值混进成本报表比缺数据更危险，估算口径留给预算器。
 * {@code tokenCacheRead} 单独成列供前缀缓存命中观测，预算记账不读它（预算是花钱口径，命中 token 也计费只是更便宜）。
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
            int tokenCacheRead,
            boolean success,
            @Nullable String errorMsg);
}
