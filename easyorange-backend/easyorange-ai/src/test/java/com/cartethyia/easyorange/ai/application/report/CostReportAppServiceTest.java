package com.cartethyia.easyorange.ai.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.dto.CostReportRow;
import com.cartethyia.easyorange.ai.application.port.query.CostReportPort;
import com.cartethyia.easyorange.ai.config.AiPricingProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CostReportAppService -> 测试")
class CostReportAppServiceTest {

    private final CostReportPort port = mock(CostReportPort.class);

    private static AiPricingProperties pricing(AiPricingProperties.ModelPrice price) {
        return new AiPricingProperties(Map.of("DecisionChatModel", price));
    }

    @Test
    @DisplayName("时间窗 -> 非法值兜底 24h，超上限收敛到 30 天")
    void clampWindow() {
        assertThat(CostReportAppService.clampWindow(0)).isEqualTo(24);
        assertThat(CostReportAppService.clampWindow(-5)).isEqualTo(24);
        assertThat(CostReportAppService.clampWindow(6)).isEqualTo(6);
        assertThat(CostReportAppService.clampWindow(24 * 365)).isEqualTo(CostReportAppService.MAX_WINDOW_HOURS);
    }

    @Test
    @DisplayName("时间窗先夹取再交给读端口（超上限的入参不会原样打到 SQL）")
    void report_clampsWindowBeforeQuerying() {
        var service = new CostReportAppService(
                port, pricing(new AiPricingProperties.ModelPrice(BigDecimal.TEN, BigDecimal.TEN, null)));
        when(port.report(CostReportAppService.MAX_WINDOW_HOURS)).thenReturn(List.of());

        service.report(24 * 365);

        verify(port).report(CostReportAppService.MAX_WINDOW_HOURS);
    }

    @Test
    @DisplayName("货币化 -> 输入/输出分别按百万 token 单价折算（入 2 元/百万 + 出 8 元/百万：1k 入 500 出 = 0.006 元）")
    void report_pricesTokensByModel() {
        var service = new CostReportAppService(
                port, pricing(new AiPricingProperties.ModelPrice(new BigDecimal("2"), new BigDecimal("8"), null)));
        when(port.report(24)).thenReturn(List.of(row("DecisionChatModel", 1000, 500, 0)));

        var rows = service.report(24);

        assertThat(rows.get(0).costYuan()).isEqualByComparingTo(new BigDecimal("0.006"));
    }

    @Test
    @DisplayName("命中档单价 -> 命中段按 hit 价、未命中段按原价（入 2 元/百万、命中 0.5 元/百万：1k 入含 600 命中 + 500 出 = 0.0051 元）")
    void report_pricesCacheHitTier() {
        var service = new CostReportAppService(
                port,
                pricing(new AiPricingProperties.ModelPrice(
                        new BigDecimal("2"), new BigDecimal("8"), new BigDecimal("0.5"))));
        when(port.report(24)).thenReturn(List.of(row("DecisionChatModel", 1000, 500, 600)));

        var rows = service.report(24);

        assertThat(rows.get(0).costYuan()).isEqualByComparingTo(new BigDecimal("0.0051"));
    }

    @Test
    @DisplayName("命中数超过输入总数 -> 按输入总数夹取（回报异常时未命中段不会算成负数）")
    void report_cacheReadClampedToInput() {
        var service = new CostReportAppService(
                port,
                pricing(new AiPricingProperties.ModelPrice(
                        new BigDecimal("2"), new BigDecimal("8"), new BigDecimal("0.5"))));
        when(port.report(24)).thenReturn(List.of(row("DecisionChatModel", 1000, 500, 2000)));

        var rows = service.report(24);

        // 2000 命中夹到 1000 全段按 hit 价：1000×0.5 + 500×8 = 4500 / 1e6
        assertThat(rows.get(0).costYuan()).isEqualByComparingTo(new BigDecimal("0.0045"));
    }

    @Test
    @DisplayName("单价表缺该模型 -> costYuan 保持 null（只出 token 不出钱，宁缺不估）")
    void report_missingPrice_keepsNullCost() {
        var service = new CostReportAppService(port, new AiPricingProperties(Map.of()));
        when(port.report(24)).thenReturn(List.of(row("UnknownModel", 1000, 500, 0)));

        var rows = service.report(24);

        assertThat(rows.get(0).costYuan()).isNull();
        assertThat(rows.get(0).totalTokens()).isEqualTo(1500);
    }

    private static CostReportRow row(String model, long tokenInput, long tokenOutput, long tokenCacheRead) {
        return new CostReportRow("CHAT", model, 3, tokenInput, tokenOutput, tokenCacheRead, 1200, 0, null);
    }
}
