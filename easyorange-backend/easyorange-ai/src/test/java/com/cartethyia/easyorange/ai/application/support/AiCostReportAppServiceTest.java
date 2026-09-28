package com.cartethyia.easyorange.ai.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.port.query.AiCostReportPort;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiCostReportAppService -> 测试")
class AiCostReportAppServiceTest {

    private final AiCostReportPort port = mock(AiCostReportPort.class);

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
        var service = new AiCostReportAppService(port);
        when(port.report(AiCostReportAppService.MAX_WINDOW_HOURS)).thenReturn(List.of());

        service.report(24 * 365);

        verify(port).report(AiCostReportAppService.MAX_WINDOW_HOURS);
    }
}
