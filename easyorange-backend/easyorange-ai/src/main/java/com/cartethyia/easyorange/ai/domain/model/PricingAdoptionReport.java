package com.cartethyia.easyorange.ai.domain.model;

/**
 * AI 建议价采纳情况 —— 全项目唯一**不依赖 LLM 评 LLM** 的质量数字。
 * <p>
 * 检索指标有语料免责（语料与 topK 同量级时 hit@5 恒为 100%）、Judge 均分有自评偏差，
 * 只有「AI 建议价 vs 资产方最终价」是人工用脚投票投出来的真实反馈：改了就说明建议没用。
 *
 * @param samples             有 AI 建议价的商品数（分母；资产方没走拍照识别的不计入）
 * @param adopted             完全采纳数（最终价与建议价一致）
 * @param adoptionRate        采纳率 = adopted / samples（samples 为 0 时记 0）
 * @param within10Percent     偏离 ≤10% 的条数（「参考了建议但自己微调」）
 * @param beyond30Percent     偏离 >30% 的条数（「基本否掉了建议」，最该看的失败面）
 * @param avgDeviationPercent 平均绝对偏离百分比
 */
public record PricingAdoptionReport(
        long samples,
        long adopted,
        double adoptionRate,
        long within10Percent,
        long beyond30Percent,
        double avgDeviationPercent) {}
