package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminReportResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ReportHandleHistoryResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ReportStatsResponse;
import com.cartethyia.easyorange.admin.service.AdminReportService;
import com.cartethyia.easyorange.common.result.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminReportService adminReportService;

    @Test
    void listReports_shouldReturnPaginatedReports() throws Exception {
        var reports = List.of(
            new AdminReportResponse("1", "100", "Product1", null, "10", "reporter1",
                1, "虚假信息", "描述", 0, "待处理", null, null,
                LocalDateTime.of(2026, 5, 16, 10, 0), null),
            new AdminReportResponse("2", "101", "Product2", null, "11", "reporter2",
                2, "侵权投诉", "描述2", 1, "处理中", null, null,
                LocalDateTime.of(2026, 5, 16, 11, 0), null)
        );
        var pageResult = PageResult.of(reports, 2L, 1, 20);
        when(adminReportService.listReports(anyInt(), anyInt(), isNull())).thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/reports"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.records.length()").value(2))
            .andExpect(jsonPath("$.data.records[0].reportId").value("1"))
            .andExpect(jsonPath("$.data.records[0].status").value(0))
            .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void listReports_withStatusFilter_shouldFilterByStatus() throws Exception {
        var reports = List.of(
            new AdminReportResponse("2", "101", "Product2", null, "11", "reporter2",
                2, "侵权投诉", "描述", 1, "处理中", null, null,
                LocalDateTime.of(2026, 5, 16, 11, 0), null)
        );
        var pageResult = PageResult.of(reports, 1L, 1, 20);
        when(adminReportService.listReports(anyInt(), anyInt(), eq(1))).thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/reports?status=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records[0].status").value(1));
    }

    @Test
    void getReportDetail_shouldReturnReport() throws Exception {
        var report = new AdminReportResponse("1", "100", "Product1", null, "10", "reporter1",
            1, "虚假信息", "描述", 0, "待处理", null, null,
            LocalDateTime.of(2026, 5, 16, 10, 0), null);
        when(adminReportService.getReportDetail("1")).thenReturn(report);

        mockMvc.perform(get("/api/admin/reports/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.reportId").value("1"))
            .andExpect(jsonPath("$.data.productName").value("Product1"))
            .andExpect(jsonPath("$.data.reasonType").value(1));
    }

    @Test
    void getReportHistory_shouldReturnHistoryList() throws Exception {
        var history = List.of(
            ReportHandleHistoryResponse.builder()
                .id("1").reportId("1").operatorName("admin")
                .action("resolve").actionDesc("处理通过")
                .remark("举报已处理").createTime(LocalDateTime.of(2026, 5, 16, 12, 0))
                .build()
        );
        when(adminReportService.getReportHistory("1")).thenReturn(history);

        mockMvc.perform(get("/api/admin/reports/1/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data[0].id").value("1"))
            .andExpect(jsonPath("$.data[0].action").value("resolve"))
            .andExpect(jsonPath("$.data[0].actionDesc").value("处理通过"));
    }

    @Test
    void handleReport_validAction_shouldSucceed() throws Exception {
        doNothing().when(adminReportService).handleReport(anyString(), any());

        mockMvc.perform(put("/api/admin/reports/1/handle")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\": \"resolve\", \"remark\": \"已核实处理\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void handleReport_invalidAction_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/admin/reports/1/handle")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\": \"invalid_action\", \"remark\": \"test\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void handleReport_withoutAction_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/admin/reports/1/handle")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"remark\": \"test\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void batchHandleReports_shouldSucceed() throws Exception {
        doNothing().when(adminReportService).batchHandleReports(any());

        mockMvc.perform(put("/api/admin/reports/batch-handle")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reportIds\": [\"1\", \"2\", \"3\"], \"action\": \"dismiss\", \"remark\": \"批量驳回\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void batchHandleReports_emptyIds_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/admin/reports/batch-handle")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reportIds\": [], \"action\": \"dismiss\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getReportStats_shouldReturnStats() throws Exception {
        var stats = ReportStatsResponse.builder()
            .totalReports(100L).pendingReports(30L).processingReports(10L)
            .resolvedReports(40L).dismissedReports(20L)
            .build();
        when(adminReportService.getReportStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/reports/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.totalReports").value(100))
            .andExpect(jsonPath("$.data.pendingReports").value(30))
            .andExpect(jsonPath("$.data.processingReports").value(10))
            .andExpect(jsonPath("$.data.resolvedReports").value(40))
            .andExpect(jsonPath("$.data.dismissedReports").value(20));
    }
}