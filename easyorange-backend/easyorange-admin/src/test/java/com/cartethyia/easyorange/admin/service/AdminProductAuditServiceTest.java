package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AuditLogResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchAuditResultResponse;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductApprovedResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductCreatedResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductSubmittedForReviewResult;
import com.cartethyia.easyorange.product.domain.entity.ProductAuditLog;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ConsignmentMode;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.event.ProductAuditedEvent;
import com.cartethyia.easyorange.product.domain.exception.InvalidProductStatusException;
import com.cartethyia.easyorange.product.domain.repository.ProductAuditLogRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductAuditService 单元测试")
class AdminProductAuditServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAuditLogRepository productAuditLogRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AdminProductAuditService auditService;

    private static final Long PRODUCT_ID = 100L;
    private static final Long OPERATOR_ID = 1L;
    private static final Long SELLER_ID = 2L;

    private Product createProductInPendingReview() {
        ProductCreatedResult result = Product.create(
                SellerId.of(SELLER_ID),
                CategoryId.of(1L),
                ProductTitle.of("测试商品"),
                Money.of(new BigDecimal("99.99")),
                null, null, ConsignmentMode.MANUAL,
                StockQuantity.of(10),
                ConditionLevel.GOOD,
                TradeLocation.of("北京"),
                ContactMethod.of("微信"),
                ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg"))
        );
        Product product = result.product().assignId(PRODUCT_ID);
        ProductSubmittedForReviewResult submitted = product.submitForReview(SELLER_ID);
        return submitted.product();
    }

    private Product createProductWithStatus(ProductStatus status) {
        ProductCreatedResult result = Product.create(
                SellerId.of(SELLER_ID),
                CategoryId.of(1L),
                ProductTitle.of("测试商品"),
                Money.of(new BigDecimal("99.99")),
                null, null, ConsignmentMode.MANUAL,
                StockQuantity.of(10),
                ConditionLevel.GOOD,
                TradeLocation.of("北京"),
                ContactMethod.of("微信"),
                ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg"))
        );
        Product product = result.product().assignId(PRODUCT_ID);
        if (status == ProductStatus.PENDING_REVIEW) {
            return product.submitForReview(SELLER_ID).product();
        }
        if (status == ProductStatus.ONLINE) {
            var pending = product.submitForReview(SELLER_ID);
            return pending.product().approve(null).product();
        }
        return product;
    }

    @Nested
    @DisplayName("auditProduct")
    class AuditProductTests {

        @Test
        @DisplayName("审核通过 — 商品状态变为上架")
        void auditProduct_approve_setsOnline() {
            when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                    .thenReturn(Optional.of(createProductInPendingReview()));

            ProductAuditRequest request = new ProductAuditRequest(1, null, null, null);

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {
                auditService.auditProduct(PRODUCT_ID, request);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }

            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).update(productCaptor.capture());
            assertThat(productCaptor.getValue().getStatus()).isEqualTo(ProductStatus.ONLINE);

            verify(productAuditLogRepository).save(any(ProductAuditLog.class));
            verify(domainEventPublisher).publish(any(ProductAuditedEvent.class));
        }

        @Test
        @DisplayName("审核拒绝 — 商品状态变为驳回")
        void auditProduct_reject_setsRejected() {
            when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                    .thenReturn(Optional.of(createProductInPendingReview()));

            ProductAuditRequest request = new ProductAuditRequest(2, "商品信息不完整", null, null);

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {
                auditService.auditProduct(PRODUCT_ID, request);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }

            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).update(productCaptor.capture());
            assertThat(productCaptor.getValue().getStatus()).isEqualTo(ProductStatus.REJECTED);

            verify(productAuditLogRepository).save(any(ProductAuditLog.class));
            verify(domainEventPublisher).publish(any(ProductAuditedEvent.class));
        }

        @Test
        @DisplayName("拒绝时未填写原因抛出异常")
        void auditProduct_rejectWithoutReason_throws() {
            when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                    .thenReturn(Optional.of(createProductInPendingReview()));

            ProductAuditRequest request = new ProductAuditRequest(2, null, null, null);

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {
                assertThatThrownBy(() -> auditService.auditProduct(PRODUCT_ID, request))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("拒绝时必须填写原因");
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("商品不存在时抛出异常")
        void auditProduct_productNotFound_throws() {
            when(productRepository.findById(ProductId.of(PRODUCT_ID))).thenReturn(Optional.empty());

            ProductAuditRequest request = new ProductAuditRequest(1, null, null, null);

            assertThatThrownBy(() -> auditService.auditProduct(PRODUCT_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("商品不存在");
        }

        @Test
        @DisplayName("非待审核状态的商品不能审核")
        void auditProduct_notPendingReview_throws() {
            Product onlineProduct = createProductWithStatus(ProductStatus.ONLINE);
            when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                    .thenReturn(Optional.of(onlineProduct));

            ProductAuditRequest request = new ProductAuditRequest(1, null, null, null);

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {
                assertThatThrownBy(() -> auditService.auditProduct(PRODUCT_ID, request))
                        .isInstanceOf(InvalidProductStatusException.class);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("batchAudit")
    class BatchAuditTests {

        @Test
        @DisplayName("批量审核成功")
        void batchAudit_mixOfSuccessAndFailure() {
            Product product1 = createProductInPendingReview();
            Product product2 = createProductInPendingReview();

            // product1 returned for ID 100 (via ProductId), product2 returned for ID 101
            // The createProductInPendingReview uses PRODUCT_ID, so we need two distinct products
            Product p1 = product1;
            Product p2 = product2;

            when(productRepository.findById(ProductId.of(100L))).thenReturn(Optional.of(p1));
            when(productRepository.findById(ProductId.of(101L))).thenReturn(Optional.of(p2));

            BatchAuditRequest request = new BatchAuditRequest();
            request.setItems(List.of(
                    new BatchAuditRequest.AuditItem(100L, 1, "通过", null),
                    new BatchAuditRequest.AuditItem(101L, 2, "信息不符", null)
            ));

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {
                BatchAuditResultResponse result = auditService.batchAudit(request);

                assertThat(result.success()).isEqualTo(2);
                assertThat(result.failed()).isZero();
                verify(productRepository, times(2)).update(any(Product.class));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("批量审核中跳过不存在的商品")
        void batchAudit_skipNotFound() {
            when(productRepository.findById(ProductId.of(100L))).thenReturn(Optional.empty());
            when(productRepository.findById(ProductId.of(101L))).thenReturn(Optional.of(createProductInPendingReview()));

            BatchAuditRequest request = new BatchAuditRequest();
            request.setItems(List.of(
                    new BatchAuditRequest.AuditItem(100L, 1, "通过", null),
                    new BatchAuditRequest.AuditItem(101L, 1, "通过", null)
            ));

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {
                BatchAuditResultResponse result = auditService.batchAudit(request);

                assertThat(result.success()).isEqualTo(1);
                assertThat(result.failed()).isEqualTo(1);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("getAuditLogs")
    class GetAuditLogsTests {

        @Test
        @DisplayName("获取审核记录列表")
        void getAuditLogs_returnsLogs() {
            ProductAuditLog log = ProductAuditLog.builder()
                    .productId(PRODUCT_ID)
                    .operatorId(OPERATOR_ID)
                    .operatorName("管理员")
                    .action(1)
                    .beforeStatus(4)
                    .afterStatus(1)
                    .build();

            when(productAuditLogRepository.findByProductId(PRODUCT_ID)).thenReturn(List.of(log));

            List<AuditLogResponse> logs = auditService.getAuditLogs(PRODUCT_ID);

            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).productId()).isEqualTo(PRODUCT_ID);
            assertThat(logs.get(0).action()).isEqualTo(1);
            assertThat(logs.get(0).actionDesc()).isEqualTo("通过");
        }

        @Test
        @DisplayName("没有审核记录时返回空列表")
        void getAuditLogs_empty_returnsEmptyList() {
            when(productAuditLogRepository.findByProductId(PRODUCT_ID)).thenReturn(List.of());

            List<AuditLogResponse> logs = auditService.getAuditLogs(PRODUCT_ID);

            assertThat(logs).isEmpty();
        }
    }
}
