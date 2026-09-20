package com.cartethyia.easyorange.ai.domain.model;

import java.util.List;

/**
 * AI 建议采纳情况（字段级）— 全项目唯一**不依赖 LLM 评 LLM** 的质量数字。
 * <p>
 * 检索指标有语料免责（语料与 topK 同量级时 hit@5 恒为 100%）、Judge 均分有自评偏差，
 * 只有「AI 建议了什么 vs 资产方最后填了什么」是人工用脚投票投出来的真实反馈。
 * <p>
 * 从「只看价格」升级为按字段看：价格改了就说明估价没用，标题被整句重写说明文案没用 ——
 * 哪个字段最不被采信，比一个笼统的总采纳率更能指出下一步该改哪里。
 *
 * @param samples             走过拍照识别的商品数（分母；没识别的不计入，否则采纳率被稀释成没有意义的数）
 * @param fullyAdopted        六个字段全部原样采纳的商品数
 * @param fullAdoptionRate    fullyAdopted / samples（samples 为 0 时记 0）
 * @param fields              逐字段采纳分布
 * @param within10Percent     建议价偏离 ≤10% 的条数（「参考了建议但自己微调」）
 * @param beyond30Percent     建议价偏离 >30% 的条数（「基本否掉了建议」，最该看的失败面）
 * @param avgPriceDeviationPercent 建议价平均绝对偏离百分比
 */
public record AiListingAdoptionReport(
        long samples,
        long fullyAdopted,
        double fullAdoptionRate,
        List<FieldAdoption> fields,
        long within10Percent,
        long beyond30Percent,
        double avgPriceDeviationPercent) {

    /**
     * 单字段采纳情况。
     *
     * @param field        字段名（title / description / price / categoryName / conditionLevel / location）
     * @param adopted      与建议完全一致的条数
     * @param adoptionRate adopted / samples（samples 为 0 时记 0）
     */
    public record FieldAdoption(String field, long adopted, double adoptionRate) {}
}
