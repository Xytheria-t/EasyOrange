package com.cartethyia.easyorange.ai.application.report;

import com.cartethyia.easyorange.ai.application.dto.AiCostReportRow;
import com.cartethyia.easyorange.ai.application.port.query.AiCostReportPort;
import com.cartethyia.easyorange.ai.config.AiPricingProperties;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI 成本报表 — 按「场景 × 模型」聚合调用次数、token 用量、平均耗时与失败次数，并按单价表货币化。
 * <p>
 * 本类做两件治理决策：窗口夹取（窗口是入参，合法区间不是）与货币化（token → 元，单价来自
 * {@link AiPricingProperties}）；聚合口径与 SQL 在 {@link AiCostReportPort} 的实现里，应用层不碰持久化。
 */
@Service
@RequiredArgsConstructor
public class AiCostReportAppService {

    /** 时间窗上限 30 天：再宽的窗口在单表上就是全表扫描，且超出「近况」的语义。 */
    static final int MAX_WINDOW_HOURS = 24 * 30;

    private final AiCostReportPort costReportPort;
    private final AiPricingProperties pricing;

    public List<AiCostReportRow> report(int hours) {
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

    /** 按单价表折算货币成本；单价表缺该模型的条目时保留 null —— 只出 token 不出钱，宁缺不估。 */
    private AiCostReportRow withCost(AiCostReportRow row) {
        var price = pricing.models().get(row.model());
        if (price == null) {
            return row;
        }
        BigDecimal cost = price.inputPerMillion()
                .multiply(BigDecimal.valueOf(row.tokenInput()))
                .add(price.outputPerMillion().multiply(BigDecimal.valueOf(row.tokenOutput())))
                .movePointLeft(6);
        return new AiCostReportRow(
                row.scope(),
                row.model(),
                row.calls(),
                row.tokenInput(),
                row.tokenOutput(),
                row.avgLatencyMs(),
                row.failures(),
                cost);
    }
}
