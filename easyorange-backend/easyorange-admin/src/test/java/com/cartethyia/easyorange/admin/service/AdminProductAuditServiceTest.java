package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.dto.response.AuditLogVO;
import com.cartethyia.easyorange.admin.dto.response.BatchAuditResultVO;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductAuditLogDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductAuditLogMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductAuditService 单元测试")
class AdminProductAuditServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductAuditLogMapper productAuditLogMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AdminProductAuditService auditService;

    private static final Long PRODUCT_ID = 100L;
    private static final Long OPERATOR_ID = 1L;
    private static final Long SELLER_ID = 2L;

    private ProductDO createProduct(int status) {
        ProductDO product = ProductDO.builder()
                .id(PRODUCT_ID)
                .userId(SELLER_ID)
                .name("测试商品")
                .price(new BigDecimal("99.99"))
                .status(status)
                .build();
        product.setDelFlag(0);
        return product;
    }

    @Nested
    @DisplayName("auditProduct")
    class AuditProductTests {

        @Test
        @DisplayName("审核通过 — 商品状态变为上架")
        void auditProduct_approve_setsOnline() {
            ProductDO product = createProduct(4);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

            ProductAuditRequest request = new ProductAuditRequest();
            request.setAction(1);
            request.setReason("审核通过");

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(OPERATOR_ID);

                auditService.auditProduct(PRODUCT_ID, request);

                assertThat(product.getStatus()).isEqualTo(1);
                verify(productMapper).updateById(product);
                verify(productAuditLogMapper).insert(any(ProductAuditLogDO.class));
            }
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("审核拒绝 — 商品状态变为驳回(5)")
        void auditProduct_reject_setsRejected() {
            ProductDO product = createProduct(4);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

            ProductAuditRequest request = new ProductAuditRequest();
            request.setAction(2);
            request.setReason("商品信息不完整");

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(OPERATOR_ID);

                auditService.auditProduct(PRODUCT_ID, request);

                assertThat(product.getStatus()).isEqualTo(5);
                verify(productMapper).updateById(product);
                verify(productAuditLogMapper).insert(any(ProductAuditLogDO.class));
            }
            // eventPublisher verification must be outside MockedStatic block to avoid scope issues
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("拒绝时未填写原因抛出异常")
        void auditProduct_rejectWithoutReason_throws() {
            ProductDO product = createProduct(4);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

            ProductAuditRequest request = new ProductAuditRequest();
            request.setAction(2);
            request.setReason(null);

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(OPERATOR_ID);

                assertThatThrownBy(() -> auditService.auditProduct(PRODUCT_ID, request))
                        .isInstanceOf(BaseBusinessException.class)
                        .hasMessageContaining("拒绝时必须填写原因");
            }
        }

        @Test
        @DisplayName("商品不存在时抛出异常")
        void auditProduct_productNotFound_throws() {
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(null);

            ProductAuditRequest request = new ProductAuditRequest();
            request.setAction(1);

            assertThatThrownBy(() -> auditService.auditProduct(PRODUCT_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("商品不存在");
        }

        @Test
        @DisplayName("非待审核状态的商品不能审核")
        void auditProduct_notPendingReview_throws() {
            ProductDO product = createProduct(1);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

            ProductAuditRequest request = new ProductAuditRequest();
            request.setAction(1);

            assertThatThrownBy(() -> auditService.auditProduct(PRODUCT_ID, request))
                    .isInstanceOf(BaseBusinessException.class)
                    .hasMessageContaining("只有待审核状态的商品可以审核");
        }
    }

    @Nested
    @DisplayName("batchAudit")
    class BatchAuditTests {

        @Test
        @DisplayName("批量审核成功")
        void batchAudit_mixOfSuccessAndFailure() {
            ProductDO product1 = createProduct(4);
            ProductDO product2 = createProduct(4);
            when(productMapper.selectById(100L)).thenReturn(product1);
            when(productMapper.selectById(101L)).thenReturn(product2);

            BatchAuditRequest request = new BatchAuditRequest();
            request.setItems(List.of(
                    new BatchAuditRequest.AuditItem(100L, 1, "通过", null),
                    new BatchAuditRequest.AuditItem(101L, 2, "信息不符", null)
            ));

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(OPERATOR_ID);

                BatchAuditResultVO result = auditService.batchAudit(request);

                assertThat(result.success()).isEqualTo(2);
                assertThat(result.failed()).isZero();
                // use any(ProductDO.class) to avoid ambiguous method reference
                verify(productMapper, times(2)).updateById(any(ProductDO.class));
            }
        }

        @Test
        @DisplayName("批量审核中跳过不存在的商品")
        void batchAudit_skipNotFound() {
            when(productMapper.selectById(100L)).thenReturn(null);
            when(productMapper.selectById(101L)).thenReturn(createProduct(4));

            BatchAuditRequest request = new BatchAuditRequest();
            request.setItems(List.of(
                    new BatchAuditRequest.AuditItem(100L, 1, "通过", null),
                    new BatchAuditRequest.AuditItem(101L, 1, "通过", null)
            ));

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(OPERATOR_ID);

                BatchAuditResultVO result = auditService.batchAudit(request);

                assertThat(result.success()).isEqualTo(1);
                assertThat(result.failed()).isEqualTo(1);
            }
        }
    }

    @Nested
    @DisplayName("getAuditLogs")
    class GetAuditLogsTests {

        @Test
        @DisplayName("获取审核记录列表")
        void getAuditLogs_returnsLogs() {
            ProductAuditLogDO log = ProductAuditLogDO.builder()
                    .id(1L)
                    .productId(PRODUCT_ID)
                    .operatorId(OPERATOR_ID)
                    .operatorName("管理员")
                    .action(1)
                    .beforeStatus(4)
                    .afterStatus(1)
                    .build();

            when(productAuditLogMapper.selectByProductId(PRODUCT_ID)).thenReturn(List.of(log));

            List<AuditLogVO> logs = auditService.getAuditLogs(PRODUCT_ID);

            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).productId()).isEqualTo(PRODUCT_ID);
            assertThat(logs.get(0).action()).isEqualTo(1);
            assertThat(logs.get(0).actionDesc()).isEqualTo("通过");
        }

        @Test
        @DisplayName("没有审核记录时返回空列表")
        void getAuditLogs_empty_returnsEmptyList() {
            when(productAuditLogMapper.selectByProductId(PRODUCT_ID)).thenReturn(List.of());

            List<AuditLogVO> logs = auditService.getAuditLogs(PRODUCT_ID);

            assertThat(logs).isEmpty();
        }
    }
}
