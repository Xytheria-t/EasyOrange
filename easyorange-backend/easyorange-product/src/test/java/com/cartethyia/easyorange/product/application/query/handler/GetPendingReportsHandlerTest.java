package com.cartethyia.easyorange.product.application.query.handler;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPendingReportsHandler 测试")
class GetPendingReportsHandlerTest {

    @Mock
    private ProductReportRepository productReportRepository;

    private GetPendingReportsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetPendingReportsHandler(productReportRepository);
    }

    @Test
    @DisplayName("获取待处理举报列表应返回分页结果")
    void handle_shouldReturnPendingReports() {
        ProductReport report1 = ProductReport.create("1", "2", "假货", 1);
        report1 = report1.assignId("100");
        ProductReport report2 = ProductReport.create("3", "4", "侵权", 2);
        report2 = report2.assignId("101");

        when(productReportRepository.findPendingReports(1, 20)).thenReturn(List.of(report1, report2));
        when(productReportRepository.countPendingReports()).thenReturn(2L);

        PageResult<ProductReportResponse> result = handler.handle(1, 20);

        assertThat(result).isNotNull();
        assertThat(result.records()).hasSize(2);
        assertThat(result.total()).isEqualTo(2L);
        assertThat(result.records().get(0).getId()).isEqualTo("100");
        assertThat(result.records().get(0).getReason()).isEqualTo("假货");
        assertThat(result.records().get(0).getStatus()).isEqualTo(0);
    }

    @Test
    @DisplayName("没有待处理举报时应返回空分页")
    void handle_withNoPendingReports_shouldReturnEmptyPage() {
        when(productReportRepository.findPendingReports(1, 20)).thenReturn(List.of());
        when(productReportRepository.countPendingReports()).thenReturn(0L);

        PageResult<ProductReportResponse> result = handler.handle(1, 20);

        assertThat(result.records()).isEmpty();
        assertThat(result.total()).isZero();
    }
}
