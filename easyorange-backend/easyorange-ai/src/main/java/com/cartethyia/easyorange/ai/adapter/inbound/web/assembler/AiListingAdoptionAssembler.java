package com.cartethyia.easyorange.ai.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.ai.adapter.inbound.web.dto.response.AiListingAdoptionVO;
import com.cartethyia.easyorange.ai.domain.model.AiListingAdoptionReport;

/**
 * 建议采纳率视图组装 — domain 报告 → 对外 VO，字段级列表一并降一层。
 */
public final class AiListingAdoptionAssembler {

    private AiListingAdoptionAssembler() {}

    public static AiListingAdoptionVO toVO(AiListingAdoptionReport report) {
        return new AiListingAdoptionVO(
                report.samples(),
                report.fullyAdopted(),
                report.fullAdoptionRate(),
                report.fields().stream()
                        .map(f -> new AiListingAdoptionVO.FieldAdoptionVO(f.field(), f.adopted(), f.adoptionRate()))
                        .toList(),
                report.within10Percent(),
                report.beyond30Percent(),
                report.avgPriceDeviationPercent());
    }
}
