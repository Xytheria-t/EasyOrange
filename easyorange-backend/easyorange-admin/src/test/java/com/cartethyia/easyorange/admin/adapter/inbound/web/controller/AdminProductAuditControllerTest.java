package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AuditLogResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchAuditResultResponse;
import com.cartethyia.easyorange.admin.service.AdminProductAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductAuditController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminProductAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminProductAuditService adminProductAuditService;

    @Test
    void auditProduct_approve_shouldSucceed() throws Exception {
        doNothing().when(adminProductAuditService).auditProduct(eq("1"), any(ProductAuditRequest.class));

        mockMvc.perform(put("/api/admin/products/1/audit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\": 1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void auditProduct_rejectWithReason_shouldSucceed() throws Exception {
        doNothing().when(adminProductAuditService).auditProduct(eq("1"), any(ProductAuditRequest.class));

        mockMvc.perform(put("/api/admin/products/1/audit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\": 2, \"reason\": \"信息不完整\", \"dimensions\": [\"description\", \"images\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void auditProduct_withoutAction_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/admin/products/1/audit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"信息不完整\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void batchAudit_shouldReturnResult() throws Exception {
        var result = new BatchAuditResultResponse(3, 2, 1, List.of("商品ID 3: 不存在"));
        when(adminProductAuditService.batchAudit(any(BatchAuditRequest.class))).thenReturn(result);

        mockMvc.perform(post("/api/admin/products/batch-audit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "items": [
                            {"productId": "1", "action": 1},
                            {"productId": "2", "action": 2, "reason": "图片不合规"},
                            {"productId": "3", "action": 1}
                        ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.total").value(3))
            .andExpect(jsonPath("$.data.success").value(2))
            .andExpect(jsonPath("$.data.failed").value(1))
            .andExpect(jsonPath("$.data.errors[0]").value("商品ID 3: 不存在"));
    }

    @Test
    void batchAudit_exceedsLimit_shouldReturn400() throws Exception {
        var items = new StringBuilder("[");
        for (int i = 1; i <= 51; i++) {
            if (i > 1) items.append(",");
            items.append("{\"productId\": \"").append(i).append("\", \"action\": 1}");
        }
        items.append("]");

        mockMvc.perform(post("/api/admin/products/batch-audit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\": " + items + "}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void batchAudit_emptyItems_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/admin/products/batch-audit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\": []}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getAuditLogs_withData_shouldReturnList() throws Exception {
        var logs = List.of(
            new AuditLogResponse("1", "1", "10", "admin", 1, "通过", null, List.of(),
                "4", "待审核", "1", "上架", null, LocalDateTime.of(2026, 5, 16, 10, 0))
        );
        when(adminProductAuditService.getAuditLogs("1")).thenReturn(logs);

        mockMvc.perform(get("/api/admin/products/1/audit-logs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data[0].id").value("1"))
            .andExpect(jsonPath("$.data[0].productId").value("1"))
            .andExpect(jsonPath("$.data[0].action").value(1))
            .andExpect(jsonPath("$.data[0].actionDesc").value("通过"))
            .andExpect(jsonPath("$.data[0].beforeStatus").value(4))
            .andExpect(jsonPath("$.data[0].afterStatus").value(1));
    }

    @Test
    void getAuditLogs_empty_shouldReturnEmptyList() throws Exception {
        when(adminProductAuditService.getAuditLogs("99")).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/products/99/audit-logs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));
    }
}