package com.cartethyia.easyorange.adapter.inbound.web.response;

import java.util.List;

/**
 * AI 建议采纳率视图 — 字段级真实反馈（非 LLM 评 LLM），语义见 {@code AiListingAdoptionReport}。
 *
 * @param within10Percent 建议价偏离 ≤10% 的条数（「参考了建议但自己微调」）
 * @param beyond30Percent 建议价偏离 >30% 的条数（「基本否掉了建议」，最该看的失败面）
 */
public record AiListingAdoptionVO(
        long samples,
        long fullyAdopted,
        double fullAdoptionRate,
        List<FieldAdoptionVO> fields,
        long within10Percent,
        long beyond30Percent,
        double avgPriceDeviationPercent) {

    /** 单字段采纳情况（samples 为 0 时记 0）。 */
    public record FieldAdoptionVO(String field, long adopted, double adoptionRate) {}
}
