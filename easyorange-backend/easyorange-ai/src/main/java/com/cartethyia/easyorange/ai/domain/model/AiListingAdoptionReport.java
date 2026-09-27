package com.cartethyia.easyorange.ai.domain.model;

import java.util.List;

/**
 * AI 建议采纳情况（字段级）— 全项目唯一<b>不依赖 LLM 评 LLM</b> 的质量数字：检索指标有语料免责
 * （语料与 topK 同量级时 hit@5 恒为 100%）、Judge 均分有自评偏差，只有「AI 建议了什么 vs 资产方
 * 最后填了什么」是人工用脚投票投出来的真实反馈。按字段看比笼统总采纳率更能指出下一步该改哪里
 * —— 价格改了说明估价没用，标题被整句重写说明文案没用。
 *
 * @param within10Percent 建议价偏离 ≤10% 的条数（「参考了建议但自己微调」）
 * @param beyond30Percent 建议价偏离 >30% 的条数（「基本否掉了建议」，最该看的失败面）
 */
public record AiListingAdoptionReport(
        long samples,
        long fullyAdopted,
        double fullAdoptionRate,
        List<FieldAdoption> fields,
        long within10Percent,
        long beyond30Percent,
        double avgPriceDeviationPercent) {

    /** 单字段采纳情况（adopted / samples，samples 为 0 时记 0）。 */
    public record FieldAdoption(String field, long adopted, double adoptionRate) {}
}
