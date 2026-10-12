package com.cartethyia.easyorange.ai.application.report;

import com.cartethyia.easyorange.ai.application.dto.CostReportRow;
import com.cartethyia.easyorange.ai.application.port.query.CostReportPort;
import com.cartethyia.easyorange.ai.config.AiPricingProperties;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI 成本报表 — 两个治理决策在这一层：窗口夹取（窗口是入参，合法区间不是）与货币化（token → 元，单价来自
 * {@link AiPricingProperties}）；聚合口径与 SQL 在 {@link CostReportPort} 实现里，应用层不碰持久化。
 */
@Service
@RequiredArgsConstructor
public class CostReportAppService {

    /** 时间窗上限 30 天：再宽的窗口在单表上就是全表扫描，且超出「近况」的语义。 */
    static final int MAX_WINDOW_HOURS = 24 * 30;

    private final CostReportPort costReportPort;
    private final AiPricingProperties pricing;

    public List<CostReportRow> report(int hours) {
        return costReportPort.report(clampWindow(hours)).stream()
                .map(this::withCost)
                .toList();
    }

    static int clampWindow(int hours) {
        if (hours <= 0) {
            return 24;
        }
        return Math.min(hours, MAX_WINDOW_HOURS);
    }

    /** 按单价表折算；缺该模型条目时保持 null —— 只出 token 不出钱，宁缺不估。 */
    private CostReportRow withCost(CostReportRow row) {
        var price = pricing.models().get(row.model());
        if (price == null) {
            return row;
        }
        // 命中段按命中档单价（缺省回落未命中价）；命中数按输入总数夹取，第三方回报异常时残段不算命中
        BigDecimal hitPrice =
                price.cacheHitInputPerMillion() != null ? price.cacheHitInputPerMillion() : price.inputPerMillion();
        long cached = Math.min(row.tokenCacheRead(), row.tokenInput());
        BigDecimal inputCost = price.inputPerMillion()
                .multiply(BigDecimal.valueOf(row.tokenInput() - cached))
                .add(hitPrice.multiply(BigDecimal.valueOf(cached)));
        BigDecimal cost = inputCost
                .add(price.outputPerMillion().multiply(BigDecimal.valueOf(row.tokenOutput())))
                .movePointLeft(6);
        return new CostReportRow(
                row.scope(),
                row.model(),
                row.calls(),
                row.tokenInput(),
                row.tokenOutput(),
                row.tokenCacheRead(),
                row.avgLatencyMs(),
                row.failures(),
                cost);
    }
}
