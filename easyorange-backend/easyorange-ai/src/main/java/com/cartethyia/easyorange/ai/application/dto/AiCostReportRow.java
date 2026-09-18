package com.cartethyia.easyorange.ai.application.dto;

/**
 * AI 成本报表的一行 —— 按调用场景聚合。
 * <p>
 * 存在的意义：{@code eo_ai_call_log} 过去只有 latency_ms，只能出「谁调用得多」，出不了「谁花得多」
 * （见 doc/技术债务清单.md TD-015）。补上 token 列之后，按场景的成本排布才第一次可查 ——
 * 也才有可能回答「该砍哪个功能」这种问题。
 *
 * @param scope        调用场景（AiCallScope 名）
 * @param calls        调用次数
 * @param tokenInput   输入 token 合计（供应商未回报用量的调用记 0，不估算 —— 估算值混进成本报表比缺数据更危险）
 * @param tokenOutput  输出 token 合计（同上）
 * @param avgLatencyMs 平均耗时毫秒
 * @param failures     失败次数（success=0）
 */
public record AiCostReportRow(
        String scope, long calls, long tokenInput, long tokenOutput, long avgLatencyMs, long failures) {

    /** 总 token —— 成本排序的第一眼指标。 */
    public long totalTokens() {
        return tokenInput + tokenOutput;
    }
}
