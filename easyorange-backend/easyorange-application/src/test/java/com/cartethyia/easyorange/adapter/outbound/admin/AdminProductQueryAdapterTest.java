package com.cartethyia.easyorange.adapter.outbound.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.AuditLogRecord;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ReportRecord;
import com.cartethyia.easyorange.ai.service.AiReviewService;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductReportQueryRepository;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.ProductCreateSpec;
import com.cartethyia.easyorange.product.domain.entity.ProductAuditLog;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.event.ProductAuditedEvent;
import com.cartethyia.easyorange.product.domain.event.ReportProcessedEvent;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.repository.ProductAuditLogRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.repository.ReportHandleHistoryRepository;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductQueryAdapter 单元测试")
class AdminProductQueryAdapterTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductDetailMapper productDetailMapper;

    @Mock
    private ProductImageMapper productImageMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private CategoryQueryRepository categoryQueryRepository;

    @Mock
    private ProductQueryRepository productQueryRepository;

    @Mock
    private ProductReportQueryRepository productReportQueryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAuditLogRepository productAuditLogRepository;

    @Mock
    private ProductReportRepository productReportRepository;

    @Mock
    private ReportHandleHistoryRepository reportHandleHistoryRepository;

    @Mock
    private ProductCacheEvictionPort productCacheEvictionPort;

    @Mock
    private AiReviewService aiReviewService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AdminProductQueryAdapter adapter;

    private static final String PRODUCT_ID = "100";
    private static final String REPORT_ID = "200";
    private static final String SELLER_ID = "1";
    private static final String OPERATOR_ID = "2";

    @BeforeEach
    void setUp() {
        adapter = new AdminProductQueryAdapter(
                productMapper,
                productDetailMapper,
                productImageMapper,
                categoryMapper,
                categoryQueryRepository,
                productQueryRepository,
                productReportQueryRepository,
                productRepository,
                productAuditLogRepository,
                productReportRepository,
                reportHandleHistoryRepository,
                productCacheEvictionPort,
                aiReviewService,
                domainEventPublisher,
                new ObjectMapper(),
                jdbcTemplate);
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
            case ONLINE -> p.submitForReview(SELLER_ID).aggregate().approve(null).aggregate();
            default -> p;
        };
    }

    private ProductReport report(ProductReportStatus status) {
        return ProductReport.reconstitute(
                REPORT_ID,
                PRODUCT_ID,
                "3",
                "虚假信息",
                status,
                status == ProductReportStatus.PENDING ? null : "已处理",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusHours(1),
                "1");
    }

    @Nested
    @DisplayName("applyProductStatus")
    class ApplyProductStatusTests {

        @Test
        @DisplayName("草稿商品直接上架")
        void draft_toOnline() {
            when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                    .thenReturn(Optional.of(createProductWithStatus(ProductStatus.DRAFT)));

            adapter.applyProductStatus(PRODUCT_ID, "ONLINE");

            verify(productRepository).save(any(Product.class));
            verify(domainEventPublisher).publish(any());
            verify(productCacheEvictionPort).evictProductCache(PRODUCT_ID);
        }

        @Test
        @DisplayName("上架商品下架")
        void online_toOffline() {
            when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                    .thenReturn(Optional.of(createProductWithStatus(ProductStatus.ONLINE)));

            adapter.applyProductStatus(PRODUCT_ID, "OFFLINE");

            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("上架商品标记售出")
        void online_toSold() {
            when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                    .thenReturn(Optional.of(createProductWithStatus(ProductStatus.ONLINE)));

            adapter.applyProductStatus(PRODUCT_ID, "SOLD");

            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("无效状态码抛出业务异常")
        void invalidStatus_throws() {
            assertThatThrownBy(() -> adapter.applyProductStatus(PRODUCT_ID, "999"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无效的商品状态");
        }

        @Test
        @DisplayName("不支持的状态抛出业务异常")
        void unsupportedStatus_throws() {
            when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                    .thenReturn(Optional.of(createProductWithStatus(ProductStatus.DRAFT)));

            assertThatThrownBy(() -> adapter.applyProductStatus(PRODUCT_ID, "PENDING_REVIEW"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不支持");
        }

        @Test
        @DisplayName("商品不存在抛出业务异常")
        void productNotFound_throws() {
            when(productRepository.findById(ProductId.of(PRODUCT_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.applyProductStatus(PRODUCT_ID, "ONLINE"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("商品不存在");
        }
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

    @Nested
    @DisplayName("handleReport")
    class HandleReportTests {

        @Test
        @DisplayName("resolve 动作通过举报并发布事件")
        void resolve_succeeds() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report(ProductReportStatus.PENDING));

            adapter.handleReport(REPORT_ID, "resolve", "已核实", OPERATOR_ID);

            verify(productReportRepository).update(any(ProductReport.class));
            verify(reportHandleHistoryRepository).save(any());
            verify(domainEventPublisher).publish(any(ReportProcessedEvent.class));
        }

        @Test
        @DisplayName("dismiss 动作驳回举报")
        void dismiss_rejects() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report(ProductReportStatus.PENDING));

            adapter.handleReport(REPORT_ID, "dismiss", "", OPERATOR_ID);

            verify(productReportRepository).update(any(ProductReport.class));
        }

        @Test
        @DisplayName("PRODUCT_OFFLINE 动作下架商品并驱逐缓存")
        void productOffline_takesProductOffline() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report(ProductReportStatus.PENDING));
            when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                    .thenReturn(Optional.of(createProductWithStatus(ProductStatus.ONLINE)));

            adapter.handleReport(REPORT_ID, "PRODUCT_OFFLINE", "", OPERATOR_ID);

            verify(productRepository).save(any(Product.class));
            verify(productCacheEvictionPort).evictProductCache(PRODUCT_ID);
        }

        @Test
        @DisplayName("已处理举报抛出业务异常")
        void alreadyHandled_throws() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report(ProductReportStatus.RESOLVED));

            assertThatThrownBy(() -> adapter.handleReport(REPORT_ID, "resolve", "", OPERATOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已被处理");
        }

        @Test
        @DisplayName("举报不存在抛出业务异常")
        void notFound_throws() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(null);

            assertThatThrownBy(() -> adapter.handleReport(REPORT_ID, "resolve", "", OPERATOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("举报记录不存在");
        }

        @Test
        @DisplayName("PRODUCT_OFFLINE 动作商品不存在抛出业务异常")
        void productOffline_missingProduct_throws() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report(ProductReportStatus.PENDING));
            when(productRepository.findById(ProductId.of(PRODUCT_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.handleReport(REPORT_ID, "PRODUCT_OFFLINE", "", OPERATOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("关联商品不存在");
        }
    }

    @Nested
    @DisplayName("getReportDetail / getReportStats")
    class ReportQueryTests {

        @Test
        @DisplayName("查询举报详情")
        void getReportDetail_returnsRecord() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report(ProductReportStatus.PENDING));

            ReportRecord record = adapter.getReportDetail(REPORT_ID);

            assertThat(record).isNotNull();
            assertThat(record.id()).isEqualTo(REPORT_ID);
            assertThat(record.status()).isEqualTo("0");
            assertThat(record.statusDesc()).isEqualTo("待处理");
            assertThat(record.reasonTypeDesc()).isEqualTo("虚假信息");
            assertThat(record.pending()).isTrue();
        }

        @Test
        @DisplayName("查询举报统计")
        void getReportStats_returnsCounts() {
            when(productReportQueryRepository.countByStatus(null)).thenReturn(10L);
            when(productReportQueryRepository.countByStatus("0")).thenReturn(5L);
            when(productReportQueryRepository.countByStatus("1")).thenReturn(2L);
            when(productReportQueryRepository.countByStatus("2")).thenReturn(2L);
            when(productReportQueryRepository.countByStatus("3")).thenReturn(1L);

            var stats = adapter.getReportStats();

            assertThat(stats.total()).isEqualTo(10);
            assertThat(stats.pending()).isEqualTo(5);
            assertThat(stats.resolved()).isEqualTo(2);
            assertThat(stats.dismissed()).isEqualTo(1);
        }
    }
}
