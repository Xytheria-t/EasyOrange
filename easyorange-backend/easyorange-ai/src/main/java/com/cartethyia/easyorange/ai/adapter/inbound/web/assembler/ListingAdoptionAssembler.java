package com.cartethyia.easyorange.ai.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.ai.adapter.inbound.web.dto.response.ListingAdoptionVO;
import com.cartethyia.easyorange.ai.domain.model.ListingAdoptionReport;

/**
 * 建议采纳率视图组装 — domain 报告 → 对外 VO，字段级列表一并降一层。
 */
public final class ListingAdoptionAssembler {

    private ListingAdoptionAssembler() {}

    public static ListingAdoptionVO toVO(ListingAdoptionReport report) {
        return new ListingAdoptionVO(
                report.samples(),
                report.fullyAdopted(),
                report.fullAdoptionRate(),
                report.fields().stream()
                        .map(f -> new ListingAdoptionVO.FieldAdoptionVO(f.field(), f.adopted(), f.adoptionRate()))
                        .toList(),
                report.within10PercentCount(),
                report.beyond30PercentCount(),
                report.avgPriceDeviationPercent());
    }
}
