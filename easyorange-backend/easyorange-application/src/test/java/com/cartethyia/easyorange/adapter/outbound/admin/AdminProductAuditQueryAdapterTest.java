package com.cartethyia.easyorange.adapter.outbound.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditQueryPort.AuditLogRecord;
import com.cartethyia.easyorange.ai.service.AiReviewService;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.ProductCreateSpec;
import com.cartethyia.easyorange.product.domain.entity.ProductAuditLog;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.event.ProductAuditedEvent;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.repository.ProductAuditLogRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductAuditQueryAdapter 单元测试")
class AdminProductAuditQueryAdapterTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductDetailMapper productDetailMapper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAuditLogRepository productAuditLogRepository;

    @Mock
    private AiReviewService aiReviewService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private AdminProductAuditQueryAdapter adapter;

    private static final String PRODUCT_ID = "100";
    private static final String SELLER_ID = "1";
    private static final String OPERATOR_ID = "2";

    @BeforeEach
    void setUp() {
        adapter = new AdminProductAuditQueryAdapter(
                productMapper,
                productDetailMapper,
                productRepository,
                productAuditLogRepository,
                aiReviewService,
                domainEventPublisher,
                new ObjectMapper());
    }

    private Product createProductWithStatus(ProductStatus status) {
        var t = Product.create(new ProductCreateSpec(
                SellerId.of(SELLER_ID),
                CategoryId.of("1"),
                ProductTitle.of("测试商品"),
                Money.of(new BigDecimal("99.99")),
                null,
                StockQuantity.of(10),
                ConditionLevel.GOOD,
                TradeLocation.of("北京"),
                ContactMethod.of("微信"),
                ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg"))));
        var p = t.aggregate().assignId(PRODUCT_ID);
        return switch (status) {
            case PENDING_REVIEW -> p.submitForReview(SELLER_ID).aggregate();
            case ONLINE ->
                p.submitForReview(SELLER_ID).aggregate().approve(null).aggregate();
            default -> p;
        };
    }

    @Nested
    @DisplayName("auditProduct")
    class AuditProductTests {

        @Test
        @DisplayName("审核通过 — 保存商品与审核日志并发布事件")
        void approve_persistsAndPublishes() {
            when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                    .thenReturn(Optional.of(createProductWithStatus(ProductStatus.PENDING_REVIEW)));

            adapter.auditProduct(PRODUCT_ID, 1, null, null, List.of(), OPERATOR_ID, "管理员");

            verify(productRepository).save(any(Product.class));
            verify(productAuditLogRepository).save(any(ProductAuditLog.class));
            verify(domainEventPublisher).publish(any(ProductAuditedEvent.class));
        }

        @Test
        @DisplayName("审核拒绝时未填原因抛出业务异常")
        void rejectWithoutReason_throws() {
            when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                    .thenReturn(Optional.of(createProductWithStatus(ProductStatus.PENDING_REVIEW)));

            assertThatThrownBy(() -> adapter.auditProduct(PRODUCT_ID, 2, null, null, null, OPERATOR_ID, "管理员"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("拒绝时必须填写原因");
        }

        @Test
        @DisplayName("商品不存在抛出领域异常")
        void productNotFound_throws() {
            when(productRepository.findById(ProductId.of(PRODUCT_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.auditProduct(PRODUCT_ID, 1, null, null, null, OPERATOR_ID, "管理员"))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAuditLogs")
    class GetAuditLogsTests {

        @Test
        @DisplayName("查询审核日志并解析维度 JSON")
        void returnsLogsWithParsedDimensions() {
            ProductAuditLog log = ProductAuditLog.builder()
                    .id("1")
                    .productId(PRODUCT_ID)
                    .operatorId(OPERATOR_ID)
                    .operatorName("管理员")
                    .action("1")
                    .auditDimensions("[\"价格合理\",\"信息完整\"]")
                    .beforeStatus(ProductStatus.PENDING_REVIEW.getCode())
                    .afterStatus(ProductStatus.ONLINE.getCode())
                    .build();
            when(productAuditLogRepository.findByProductId(PRODUCT_ID)).thenReturn(List.of(log));

            List<AuditLogRecord> logs = adapter.getAuditLogs(PRODUCT_ID);

            assertThat(logs).hasSize(1);
            AuditLogRecord record = logs.get(0);
            assertThat(record.actionDesc()).isEqualTo("通过");
            assertThat(record.afterStatusDesc()).isEqualTo("上架");
            assertThat(record.dimensions()).containsExactly("价格合理", "信息完整");
        }
    }
}
