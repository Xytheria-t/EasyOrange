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

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMyReportsHandler 测试")
class GetMyReportsHandlerTest {

    @Mock
    private ProductReportRepository productReportRepository;

    private GetMyReportsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetMyReportsHandler(productReportRepository);
    }

    @Test
    @DisplayName("查询我的举报列表应返回分页结果")
    void handle_shouldReturnPaginatedReports() {
        ProductReport report1 = ProductReport.create(1L, 2L, "假货", 1);
        report1.assignId(100L);
        ProductReport report2 = ProductReport.create(1L, 3L, "侵权", 2);
        report2.assignId(101L);

        List<ProductReport> reports = List.of(report1, report2);
        PageResult<ProductReport> pageResult = PageResult.of(reports, 2L, 1, 20);
        when(productReportRepository.findByReporterId(2L, 1, 20)).thenReturn(pageResult);

        PageResult<ProductReportResponse> result = handler.handle(2L, 1, 20);

        assertThat(result).isNotNull();
        assertThat(result.records()).hasSize(2);
        assertThat(result.total()).isEqualTo(2L);
        assertThat(result.records().get(0).getId()).isEqualTo(100L);
        assertThat(result.records().get(0).getProductId()).isEqualTo(1L);
        assertThat(result.records().get(0).getReason()).isEqualTo("假货");
        assertThat(result.records().get(0).getStatus()).isEqualTo(0);
    }

    @Test
    @DisplayName("查询结果为空时应返回空分页")
    void handle_withEmptyResult_shouldReturnEmptyPage() {
        when(productReportRepository.findByReporterId(2L, 1, 20))
                .thenReturn(PageResult.of(List.of(), 0L, 1, 20));

        PageResult<ProductReportResponse> result = handler.handle(2L, 1, 20);

        assertThat(result.records()).isEmpty();
        assertThat(result.total()).isZero();
    }

    @Test
    @DisplayName("null 的举报记录应返回 null 响应")
    void toResponse_withNullReport_shouldReturnNull() {
        ProductReport report = ProductReport.create(1L, 2L, "假货", 1);
        report.assignId(100L);

        List<ProductReport> reports = Arrays.asList(report, null);
        when(productReportRepository.findByReporterId(2L, 1, 20))
                .thenReturn(PageResult.of(reports, 2L, 1, 20));

        PageResult<ProductReportResponse> result = handler.handle(2L, 1, 20);

        assertThat(result.records()).hasSize(2);
        assertThat(result.records().get(0)).isNotNull();
        assertThat(result.records().get(1)).isNull();
    }
}
