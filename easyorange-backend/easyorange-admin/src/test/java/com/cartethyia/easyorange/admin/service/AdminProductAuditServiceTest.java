package com.cartethyia.easyorange.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AuditLogResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchAuditResultResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditPort;
import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditPort.AuditLogRecord;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.security.AuthUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductAuditService 单元测试")
class AdminProductAuditServiceTest {

    @Mock
    private AdminProductAuditPort adminProductAuditPort;

    @InjectMocks
    private AdminProductAuditService auditService;

    private static final String PRODUCT_ID = "100";
    private static final String OPERATOR_ID = "1";

    private static AuthUser operator() {
        return new AuthUser(OPERATOR_ID, "管理员");
    }

    private AuditLogRecord createAuditLog() {
        return new AuditLogRecord(
                "1",
                PRODUCT_ID,
                OPERATOR_ID,
                "管理员",
                "1",
                "通过",
                null,
                List.of(),
                "PENDING_REVIEW",
                "待审核",
                "ONLINE",
                "上架",
                null,
                LocalDateTime.now());
    }

    @Nested
    @DisplayName("auditProduct")
    class AuditProductTests {

        @Test
        @DisplayName("审核通过 — 委托端口并携带操作人信息")
        void auditProduct_approve_delegatesToPort() {
            ProductAuditRequest request = new ProductAuditRequest(1, null, null, null);

            auditService.auditProduct(operator(), PRODUCT_ID, request);

            verify(adminProductAuditPort)
                    .auditProduct(eq(PRODUCT_ID), eq(1), eq(null), eq(null), eq(null), eq(OPERATOR_ID), any());
        }

        @Test
        @DisplayName("审核拒绝带原因")
        void auditProduct_reject_delegatesToPort() {
            ProductAuditRequest request = new ProductAuditRequest(2, "商品信息不完整", null, null);

            auditService.auditProduct(operator(), PRODUCT_ID, request);

            verify(adminProductAuditPort)
                    .auditProduct(eq(PRODUCT_ID), eq(2), eq("商品信息不完整"), eq(null), eq(null), eq(OPERATOR_ID), any());
        }
    }

    @Nested
    @DisplayName("batchAudit")
    class BatchAuditTests {

        @Test
        @DisplayName("批量审核成功")
        void batchAudit_allSuccess() {
            BatchAuditRequest request = new BatchAuditRequest();
            request.setItems(List.of(
                    new BatchAuditRequest.AuditItem("100", 1, "通过", null),
                    new BatchAuditRequest.AuditItem("101", 2, "信息不符", null)));

            BatchAuditResultResponse result = auditService.batchAudit(operator(), request);

            assertThat(result.success()).isEqualTo(2);
            assertThat(result.failed()).isZero();
        }

        @Test
        @DisplayName("批量审核中跳过失败项")
        void batchAudit_skipFailedItems() {
            doThrow(BusinessException.of("资产不存在"))
                    .when(adminProductAuditPort)
                    .auditProduct(eq("100"), eq(1), any(), any(), any(), any(), any());

            BatchAuditRequest request = new BatchAuditRequest();
            request.setItems(List.of(
                    new BatchAuditRequest.AuditItem("100", 1, "通过", null),
                    new BatchAuditRequest.AuditItem("101", 1, "通过", null)));

            BatchAuditResultResponse result = auditService.batchAudit(operator(), request);

            assertThat(result.success()).isEqualTo(1);
            assertThat(result.failed()).isEqualTo(1);
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0)).contains("100");
        }
    }

    @Nested
    @DisplayName("getAuditLogs")
    class GetAuditLogsTests {

        @Test
        @DisplayName("获取审核记录列表")
        void getAuditLogs_returnsLogs() {
            when(adminProductAuditPort.getAuditLogs(PRODUCT_ID)).thenReturn(List.of(createAuditLog()));

            List<AuditLogResponse> logs = auditService.getAuditLogs(PRODUCT_ID);

            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).productId()).isEqualTo(PRODUCT_ID);
            assertThat(logs.get(0).action()).isEqualTo(1);
            assertThat(logs.get(0).actionDesc()).isEqualTo("通过");
            assertThat(logs.get(0).afterStatusDesc()).isEqualTo("上架");
        }

        @Test
        @DisplayName("没有审核记录时返回空列表")
        void getAuditLogs_empty_returnsEmptyList() {
            when(adminProductAuditPort.getAuditLogs(PRODUCT_ID)).thenReturn(List.of());

            List<AuditLogResponse> logs = auditService.getAuditLogs(PRODUCT_ID);

            assertThat(logs).isEmpty();
        }
    }
}
