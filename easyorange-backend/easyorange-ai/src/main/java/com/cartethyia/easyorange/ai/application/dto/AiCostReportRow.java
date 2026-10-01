package com.cartethyia.easyorange.ai.application.dto;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * AI 成本报表的一行 —— 按「场景 × 模型」聚合。
 * <p>
 * 存在的意义：{@code eo_ai_call_log} 过去只有 latency_ms，只能出「谁调用得多」，出不了「谁花得多」
 * （见 doc/技术债务清单.md TD-015）。补上 token 列之后按场景可查，补上 model 维度与单价后才能回答
 * 「哪个模型贵、换供应商值不值」—— 决策/生成分离、语义缓存这些省钱手段的效果也才有对照数字。
 *
 * @param scope        调用场景（AiCallScope 名）
 * @param model        模型标识（eo_ai_call_log.model，供应商回报侧的 bean 简名）
 * @param calls        调用次数
 * @param tokenInput   输入 token 合计（供应商未回报用量的调用记 0，不估算 —— 估算值混进成本报表比缺数据更危险）
 * @param tokenOutput  输出 token 合计（同上）
 * @param avgLatencyMs 平均耗时毫秒
 * @param failures     失败次数（success=0）
 * @param costYuan     货币成本（元，按单价表计算；单价表缺该模型时为 null —— 只出 token 不出钱）
 */
public record AiCostReportRow(
        String scope,
        String model,
        long calls,
        long tokenInput,
        long tokenOutput,
        long avgLatencyMs,
        long failures,
        @Nullable BigDecimal costYuan) {

    /** 总 token —— 成本排序的第一眼指标。 */
    public long totalTokens() {
        return tokenInput + tokenOutput;
    }
}
