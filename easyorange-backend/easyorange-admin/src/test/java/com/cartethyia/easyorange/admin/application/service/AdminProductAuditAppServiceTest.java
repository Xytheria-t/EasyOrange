package com.cartethyia.easyorange.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.domain.model.BatchAuditItem;
import com.cartethyia.easyorange.admin.domain.model.BatchAuditResult;
import com.cartethyia.easyorange.admin.domain.model.ProductAuditCommand;
import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditPort;
import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditPort.AuditLogRecord;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.security.AuthUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductAuditAppService 单元测试")
class AdminProductAuditAppServiceTest {

    @Mock
    private AdminProductAuditPort adminProductAuditPort;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private AdminProductAuditAppService auditService;

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
            auditService.auditProduct(operator(), PRODUCT_ID, new ProductAuditCommand(1, null, null, null));

            verify(adminProductAuditPort)
                    .auditProduct(eq(PRODUCT_ID), eq(1), eq(null), eq(null), eq(null), eq(OPERATOR_ID), any());
        }

        @Test
        @DisplayName("审核拒绝带原因与备注")
        void auditProduct_reject_delegatesToPort() {
            auditService.auditProduct(operator(), PRODUCT_ID, new ProductAuditCommand(2, "商品信息不完整", "人工复核", null));

            verify(adminProductAuditPort)
                    .auditProduct(eq(PRODUCT_ID), eq(2), eq("商品信息不完整"), eq("人工复核"), eq(null), eq(OPERATOR_ID), any());
        }
    }

    @Nested
    @DisplayName("batchAudit")
    class BatchAuditTests {

        @BeforeEach
        void runTxCallbackForReal() {
            // TransactionTemplate 直通：executeWithoutResult 真实执行回调（事务边界由集成测试覆盖）
            lenient()
                    .doAnswer(invocation -> {
                        Consumer<TransactionStatus> callback = invocation.getArgument(0);
                        callback.accept(null);
                        return null;
                    })
                    .when(transactionTemplate)
                    .executeWithoutResult(any());
        }

        @Test
        @DisplayName("批量审核成功")
        void batchAudit_allSuccess() {
            List<BatchAuditItem> items =
                    List.of(new BatchAuditItem("100", 1, "通过", null), new BatchAuditItem("101", 2, "信息不符", null));

            BatchAuditResult result = auditService.batchAudit(operator(), items);

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.success()).isEqualTo(2);
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("批量审核中跳过失败项，备注位固定为 null")
        void batchAudit_skipFailedItems() {
            doThrow(BusinessException.of("资产不存在"))
                    .when(adminProductAuditPort)
                    .auditProduct(eq("100"), eq(1), any(), any(), any(), any(), any());

            List<BatchAuditItem> items =
                    List.of(new BatchAuditItem("100", 1, "通过", null), new BatchAuditItem("101", 1, "通过", null));

            BatchAuditResult result = auditService.batchAudit(operator(), items);

            assertThat(result.success()).isEqualTo(1);
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0)).contains("100");
            verify(adminProductAuditPort).auditProduct(eq("101"), eq(1), any(), eq((String) null), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getAuditLogs")
    class GetAuditLogsTests {

        @Test
        @DisplayName("获取审核记录列表")
        void getAuditLogs_returnsLogs() {
            when(adminProductAuditPort.getAuditLogs(PRODUCT_ID)).thenReturn(List.of(createAuditLog()));

            List<AuditLogRecord> logs = auditService.getAuditLogs(PRODUCT_ID);

            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).productId()).isEqualTo(PRODUCT_ID);
            assertThat(logs.get(0).action()).isEqualTo("1");
            assertThat(logs.get(0).actionDesc()).isEqualTo("通过");
            assertThat(logs.get(0).afterStatusDesc()).isEqualTo("上架");
        }

        @Test
        @DisplayName("没有审核记录时返回空列表")
        void getAuditLogs_empty_returnsEmptyList() {
            when(adminProductAuditPort.getAuditLogs(PRODUCT_ID)).thenReturn(List.of());

            assertThat(auditService.getAuditLogs(PRODUCT_ID)).isEmpty();
        }
    }
}
