package com.cartethyia.easyorange.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminReportAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchHandleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ReportHandleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminReportResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchHandleResultResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ReportHandleHistoryResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ReportStatsResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminReportQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminReportQueryPort.ReportHistoryRecord;
import com.cartethyia.easyorange.admin.domain.port.AdminReportQueryPort.ReportQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminReportQueryPort.ReportRecord;
import com.cartethyia.easyorange.admin.domain.port.AdminReportQueryPort.ReportStats;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminReportService 单元测试")
class AdminReportServiceTest {

    @Mock
    private AdminReportQueryPort adminReportQueryPort;

    @Mock
    private AdminProductQueryPort adminProductQueryPort;

    @Mock
    private AdminUserQueryPort adminUserQueryPort;

    @Spy
    private AdminReportAssembler assembler = new AdminReportAssembler();

    @InjectMocks
    private AdminReportService reportService;

    private static final String REPORT_ID = "100";
    private static final String PRODUCT_ID = "200";
    private static final String REPORTER_ID = "1";
    private static final String OPERATOR_ID = "2";

    private ReportRecord createPendingReport() {
        return new ReportRecord(
                REPORT_ID,
                PRODUCT_ID,
                REPORTER_ID,
                "1",
                "虚假信息",
                "虚假信息",
                "0",
                "待处理",
                null,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusHours(1),
                true);
    }

    private ReportRecord createResolvedReport() {
        return new ReportRecord(
                REPORT_ID,
                PRODUCT_ID,
                REPORTER_ID,
                "1",
                "虚假信息",
                "虚假信息",
                "2",
                "已解决",
                "已处理",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                false);
    }

    private AdminUserQueryPort.UserInfo createUser(String id, String name) {
        return new AdminUserQueryPort.UserInfo(id, name, name, null, null);
    }

    @Nested
    @DisplayName("listReports")
    class ListReportsTests {

        @Test
        @DisplayName("分页查询举报列表")
        void listReports_withStatus_returnsPage() {
            when(adminReportQueryPort.queryReports(0, 1, 20))
                    .thenReturn(new ReportQueryResult(List.of(createPendingReport()), 1, 1, 20));
            when(adminUserQueryPort.getUserInfos(anyList()))
                    .thenReturn(Map.of(REPORTER_ID, createUser(REPORTER_ID, "举报人")));
            when(adminProductQueryPort.getProductInfos(anyList()))
                    .thenReturn(Map.of(PRODUCT_ID, new AdminProductQueryPort.ProductInfo(PRODUCT_ID, "测试商品")));

            PageResult<AdminReportResponse> result = reportService.listReports(1, 20, 0);

            assertThat(result.records()).hasSize(1);
            AdminReportResponse vo = result.records().get(0);
            assertThat(vo.reportId()).isEqualTo(REPORT_ID);
            assertThat(vo.productName()).isEqualTo("测试商品");
            assertThat(vo.reporterName()).isEqualTo("举报人");
            assertThat(vo.status()).isEqualTo(0);
            assertThat(vo.statusDesc()).isEqualTo("待处理");
        }

        @Test
        @DisplayName("无状态筛选时不传 status 给端口")
        void listReports_withoutStatus_passesNull() {
            when(adminReportQueryPort.queryReports(null, 1, 20))
                    .thenReturn(new ReportQueryResult(List.of(createPendingReport()), 1, 1, 20));
            when(adminUserQueryPort.getUserInfos(anyList())).thenReturn(Map.of());
            when(adminProductQueryPort.getProductInfos(anyList())).thenReturn(Map.of());

            PageResult<AdminReportResponse> result = reportService.listReports(1, 20, null);

            assertThat(result.records()).hasSize(1);
            verify(adminReportQueryPort).queryReports(null, 1, 20);
        }
    }

    @Nested
    @DisplayName("getReportDetail")
    class GetReportDetailTests {

        @Test
        @DisplayName("获取举报详情成功")
        void getReportDetail_success() {
            when(adminReportQueryPort.getReportDetail(REPORT_ID)).thenReturn(createPendingReport());
            when(adminUserQueryPort.getUserInfos(anyList()))
                    .thenReturn(Map.of(REPORTER_ID, createUser(REPORTER_ID, "举报人")));
            when(adminProductQueryPort.getProductInfos(anyList()))
                    .thenReturn(Map.of(PRODUCT_ID, new AdminProductQueryPort.ProductInfo(PRODUCT_ID, "测试商品")));

            AdminReportResponse vo = reportService.getReportDetail(REPORT_ID);

            assertThat(vo).isNotNull();
            assertThat(vo.reportId()).isEqualTo(REPORT_ID);
        }

        @Test
        @DisplayName("举报不存在时抛出异常")
        void getReportDetail_notFound_throws() {
            when(adminReportQueryPort.getReportDetail(REPORT_ID)).thenReturn(null);

            assertThatThrownBy(() -> reportService.getReportDetail(REPORT_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("举报记录不存在");
        }
    }

    @Nested
    @DisplayName("handleReport")
    class HandleReportTests {

        @Test
        @DisplayName("处理举报 — 委托端口并携带操作人")
        void handleReport_delegatesToPort() {
            ReportHandleRequest request = new ReportHandleRequest();
            request.setAction("resolve");
            request.setRemark("已核实处理");

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {
                reportService.handleReport(REPORT_ID, request);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }

            verify(adminReportQueryPort).handleReport(REPORT_ID, "resolve", "已核实处理", OPERATOR_ID);
        }

        @Test
        @DisplayName("端口抛出业务异常向上传播")
        void handleReport_portThrows_propagates() {
            doThrow(BusinessException.of("该举报已被处理"))
                    .when(adminReportQueryPort)
                    .handleReport(eq(REPORT_ID), eq("resolve"), any(), any());

            ReportHandleRequest request = new ReportHandleRequest();
            request.setAction("resolve");

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {
                assertThatThrownBy(() -> reportService.handleReport(REPORT_ID, request))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("已被处理");
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("batchHandleReports")
    class BatchHandleReportsTests {

        @Test
        @DisplayName("批量处理举报返回聚合结果")
        void batchHandleReports_success() {
            BatchHandleRequest request = new BatchHandleRequest();
            request.setReportIds(List.of("100", "101"));
            request.setAction("dismiss");

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {
                BatchHandleResultResponse result = reportService.batchHandleReports(request);

                assertThat(result.total()).isEqualTo(2);
                assertThat(result.success()).isEqualTo(2);
                assertThat(result.failed()).isZero();
                assertThat(result.errors()).isEmpty();
                verify(adminReportQueryPort, org.mockito.Mockito.times(2)).handleReport(any(), any(), any(), any());
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("批量处理部分失败时聚合错误信息")
        void batchHandleReports_partialFailure_aggregatesErrors() {
            org.mockito.Mockito.lenient()
                    .doThrow(BusinessException.of("该举报已被处理"))
                    .when(adminReportQueryPort)
                    .handleReport(eq("101"), any(), any(), any());

            BatchHandleRequest request = new BatchHandleRequest();
            request.setReportIds(List.of("100", "101"));
            request.setAction("dismiss");

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {
                BatchHandleResultResponse result = reportService.batchHandleReports(request);

                assertThat(result.total()).isEqualTo(2);
                assertThat(result.success()).isEqualTo(1);
                assertThat(result.failed()).isEqualTo(1);
                assertThat(result.errors()).hasSize(1);
                assertThat(result.errors().get(0)).contains("101");
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("空列表抛出异常")
        void batchHandleReports_emptyList_throws() {
            BatchHandleRequest request = new BatchHandleRequest();
            request.setReportIds(List.of());
            request.setAction("dismiss");

            assertThatThrownBy(() -> reportService.batchHandleReports(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能为空");
        }

        @Test
        @DisplayName("超过50条抛出异常")
        void batchHandleReports_exceedLimit_throws() {
            List<String> ids = java.util.stream.LongStream.range(1, 52)
                    .mapToObj(String::valueOf)
                    .toList();
            BatchHandleRequest request = new BatchHandleRequest();
            request.setReportIds(ids);
            request.setAction("dismiss");

            assertThatThrownBy(() -> reportService.batchHandleReports(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能超过50条");
        }
    }

    @Nested
    @DisplayName("getReportStats / getReportHistory")
    class StatsAndHistoryTests {

        @Test
        @DisplayName("获取举报统计")
        void getReportStats_returnsStats() {
            when(adminReportQueryPort.getReportStats()).thenReturn(new ReportStats(10, 5, 2, 2, 1));

            ReportStatsResponse stats = reportService.getReportStats();

            assertThat(stats.totalReports()).isEqualTo(10);
            assertThat(stats.pendingReports()).isEqualTo(5);
            assertThat(stats.resolvedReports()).isEqualTo(2);
            assertThat(stats.dismissedReports()).isEqualTo(1);
        }

        @Test
        @DisplayName("获取举报处理历史")
        void getReportHistory_returnsHistory() {
            ReportHistoryRecord history =
                    new ReportHistoryRecord("1", REPORT_ID, OPERATOR_ID, "resolve", "已处理", LocalDateTime.now());

            when(adminReportQueryPort.getReportHistory(REPORT_ID)).thenReturn(List.of(history));
            when(adminUserQueryPort.getUserInfos(anyList()))
                    .thenReturn(Map.of(OPERATOR_ID, createUser(OPERATOR_ID, "管理员")));

            List<ReportHandleHistoryResponse> historyList = reportService.getReportHistory(REPORT_ID);

            assertThat(historyList).hasSize(1);
            assertThat(historyList.get(0).action()).isEqualTo("resolve");
            assertThat(historyList.get(0).actionDesc()).isEqualTo("处理通过");
        }
    }
}
