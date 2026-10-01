package com.cartethyia.easyorange.ai.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.dto.AiCostReportRow;
import com.cartethyia.easyorange.ai.application.port.query.AiCostReportPort;
import com.cartethyia.easyorange.ai.config.AiPricingProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiCostReportAppService -> 测试")
class AiCostReportAppServiceTest {

    private final AiCostReportPort port = mock(AiCostReportPort.class);

    private static AiPricingProperties pricing(AiPricingProperties.ModelPrice price) {
        return new AiPricingProperties(Map.of("DecisionChatModel", price));
    }

    @Test
    @DisplayName("时间窗 -> 非法值兜底 24h，超上限收敛到 30 天")
    void clampWindow() {
        assertThat(AiCostReportAppService.clampWindow(0)).isEqualTo(24);
        assertThat(AiCostReportAppService.clampWindow(-5)).isEqualTo(24);
        assertThat(AiCostReportAppService.clampWindow(6)).isEqualTo(6);
        assertThat(AiCostReportAppService.clampWindow(24 * 365)).isEqualTo(AiCostReportAppService.MAX_WINDOW_HOURS);
    }

    @Test
    @DisplayName("时间窗先夹取再交给读端口（超上限的入参不会原样打到 SQL）")
    void report_clampsWindowBeforeQuerying() {
        var service = new AiCostReportAppService(
                port, pricing(new AiPricingProperties.ModelPrice(BigDecimal.TEN, BigDecimal.TEN)));
        when(port.report(AiCostReportAppService.MAX_WINDOW_HOURS)).thenReturn(List.of());

        service.report(24 * 365);

        verify(port).report(AiCostReportAppService.MAX_WINDOW_HOURS);
    }

    @Test
    @DisplayName("货币化 -> 输入/输出分别按百万 token 单价折算（入 2 元/百万 + 出 8 元/百万：1k 入 500 出 = 0.006 元）")
    void report_pricesTokensByModel() {
        var service = new AiCostReportAppService(
                port, pricing(new AiPricingProperties.ModelPrice(new BigDecimal("2"), new BigDecimal("8"))));
        when(port.report(24)).thenReturn(List.of(row("DecisionChatModel", 1000, 500)));

        var rows = service.report(24);

        assertThat(rows.get(0).costYuan()).isEqualByComparingTo(new BigDecimal("0.006"));
    }

    @Test
    @DisplayName("单价表缺该模型 -> costYuan 保持 null（只出 token 不出钱，宁缺不估）")
    void report_missingPrice_keepsNullCost() {
        var service = new AiCostReportAppService(port, new AiPricingProperties(Map.of()));
        when(port.report(24)).thenReturn(List.of(row("UnknownModel", 1000, 500)));

        var rows = service.report(24);

        assertThat(rows.get(0).costYuan()).isNull();
        assertThat(rows.get(0).totalTokens()).isEqualTo(1500);
    }

    private static AiCostReportRow row(String model, long tokenInput, long tokenOutput) {
        return new AiCostReportRow("CHAT", model, 3, tokenInput, tokenOutput, 1200, 0, null);
    }
}
