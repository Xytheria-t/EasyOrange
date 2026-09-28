package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminProductAuditAssembler;
import com.cartethyia.easyorange.admin.application.service.AdminProductAuditAppService;
import com.cartethyia.easyorange.admin.domain.model.BatchAuditResult;
import com.cartethyia.easyorange.admin.domain.model.ProductAuditCommand;
import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditPort.AuditLogRecord;
import com.cartethyia.easyorange.common.security.AuthUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 后台审核接口契约测试 — 请求拆成 {@code domain} 命令、日志的 String code 翻成数字动作值，
 * 两段拼起来才是前端看到的 JSON，所以 assembler 用真身。
 */
@WebMvcTest(AdminProductAuditController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AdminProductAuditAssembler.class)
class AdminProductAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminProductAuditAppService adminProductAuditService;

    private static final String USER_ID = "10";

    @BeforeEach
    void setUp() {
        var authUser = new AuthUser(USER_ID, "admin");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(authUser, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditProduct_approve_shouldSucceed() throws Exception {
        doNothing()
                .when(adminProductAuditService)
                .auditProduct(any(AuthUser.class), eq("1"), any(ProductAuditCommand.class));

        mockMvc.perform(put("/api/admin/products/1/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void auditProduct_rejectWithReason_shouldPassCommandToService() throws Exception {
        doNothing()
                .when(adminProductAuditService)
                .auditProduct(any(AuthUser.class), eq("1"), any(ProductAuditCommand.class));

        mockMvc.perform(put("/api/admin/products/1/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": 2, \"reason\": \"信息不完整\", \"dimensions\": [\"description\","
                                + " \"images\"]}"))
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
        when(adminProductAuditService.batchAudit(any(AuthUser.class), any()))
                .thenReturn(new BatchAuditResult(3, 2, List.of("商品ID 3: 不存在")));

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
        when(adminProductAuditService.getAuditLogs("1"))
                .thenReturn(List.of(new AuditLogRecord(
                        "1",
                        "1",
                        USER_ID,
                        "admin",
                        "1",
                        "通过",
                        null,
                        List.of(),
                        "4",
                        "待审核",
                        "1",
                        "上架",
                        null,
                        LocalDateTime.of(2026, 5, 16, 10, 0))));

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
