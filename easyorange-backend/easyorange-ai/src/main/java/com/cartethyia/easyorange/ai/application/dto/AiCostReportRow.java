package com.cartethyia.easyorange.ai.application.dto;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * AI 成本报表的一行 —— 按「场景 × 模型」聚合。
 * <p>
 * 存在的意义（见 doc/技术债务清单.md TD-015）：{@code eo_ai_call_log} 只有 latency_ms 时只能出「谁调用
 * 得多」，出不了「谁花得多」；有 token、单价与 model 维度才回答得了「换供应商值不值」—— 决策/生成分离、
 * 语义缓存这些省钱手段也才有对照数字。
 *
 * @param scope         调用场景（AiCallScope 名）
 * @param model         模型标识（eo_ai_call_log.model，供应商回报侧的 bean 简名）
 * @param tokenInput    输入 token 合计（供应商未回报用量记 0，不估算）
 * @param tokenOutput   输出 token 合计（同上）
 * @param tokenCacheRead 缓存命中输入 token 合计（前缀缓存命中观测，含在 tokenInput 内；未回报记 0）
 * @param failures      失败次数（success=0）
 * @param costYuan      货币成本（元，按单价表折算；单价表缺该模型为 null）
 */
public record AiCostReportRow(
        String scope,
        String model,
        long calls,
        long tokenInput,
        long tokenOutput,
        long tokenCacheRead,
        long avgLatencyMs,
        long failures,
        @Nullable BigDecimal costYuan) {

    /** 总 token —— 成本排序的第一眼指标。 */
    public long totalTokens() {
        return tokenInput + tokenOutput;
    }
}
