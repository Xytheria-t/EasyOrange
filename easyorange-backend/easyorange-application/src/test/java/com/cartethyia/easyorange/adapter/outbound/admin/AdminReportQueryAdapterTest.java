package com.cartethyia.easyorange.adapter.outbound.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.domain.port.AdminReportQueryPort.ReportRecord;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.application.port.query.ProductReportQueryRepository;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.ProductCreateSpec;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.event.ReportProcessedEvent;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminReportQueryAdapter 单元测试")
class AdminReportQueryAdapterTest {

    @Mock
    private ProductReportQueryRepository productReportQueryRepository;

    @Mock
    private ProductReportRepository productReportRepository;

    @Mock
    private ReportHandleHistoryRepository reportHandleHistoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCacheEvictionPort productCacheEvictionPort;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private AdminReportQueryAdapter adapter;

    private static final String PRODUCT_ID = "100";
    private static final String REPORT_ID = "200";
    private static final String SELLER_ID = "1";

    @BeforeEach
    void setUp() {
        adapter = new AdminReportQueryAdapter(
                productReportQueryRepository,
                productReportRepository,
                reportHandleHistoryRepository,
                productRepository,
                productCacheEvictionPort,
                domainEventPublisher);
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
    @DisplayName("handleReport")
    class HandleReportTests {

        @Test
        @DisplayName("resolve 动作通过举报并发布事件")
        void resolve_succeeds() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report(ProductReportStatus.PENDING));

            adapter.handleReport(REPORT_ID, "resolve", "已核实", "2");

            verify(productReportRepository).update(any(ProductReport.class));
            verify(reportHandleHistoryRepository).save(any());
            verify(domainEventPublisher).publish(any(ReportProcessedEvent.class));
        }

        @Test
        @DisplayName("dismiss 动作驳回举报")
        void dismiss_rejects() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report(ProductReportStatus.PENDING));

            adapter.handleReport(REPORT_ID, "dismiss", "", "2");

            verify(productReportRepository).update(any(ProductReport.class));
        }

        @Test
        @DisplayName("PRODUCT_OFFLINE 动作下架商品并驱逐缓存")
        void productOffline_takesProductOffline() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report(ProductReportStatus.PENDING));
            when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                    .thenReturn(Optional.of(createProductWithStatus(ProductStatus.ONLINE)));

            adapter.handleReport(REPORT_ID, "PRODUCT_OFFLINE", "", "2");

            verify(productRepository).save(any(Product.class));
            verify(productCacheEvictionPort).evictProductCache(PRODUCT_ID);
        }

        @Test
        @DisplayName("已处理举报抛出业务异常")
        void alreadyHandled_throws() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report(ProductReportStatus.RESOLVED));

            assertThatThrownBy(() -> adapter.handleReport(REPORT_ID, "resolve", "", "2"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已被处理");
        }

        @Test
        @DisplayName("举报不存在抛出业务异常")
        void notFound_throws() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(null);

            assertThatThrownBy(() -> adapter.handleReport(REPORT_ID, "resolve", "", "2"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("举报记录不存在");
        }

        @Test
        @DisplayName("PRODUCT_OFFLINE 动作商品不存在抛出业务异常")
        void productOffline_missingProduct_throws() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report(ProductReportStatus.PENDING));
            when(productRepository.findById(ProductId.of(PRODUCT_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.handleReport(REPORT_ID, "PRODUCT_OFFLINE", "", "2"))
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
