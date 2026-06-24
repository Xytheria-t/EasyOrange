package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchHandleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ReportHandleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminReportResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ReportHandleHistoryResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ReportStatsResponse;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.enums.ConsignmentMode;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TagSet;
import com.cartethyia.easyorange.product.domain.valueobject.Version;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.domain.entity.ReportHandleHistory;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ReportHandleHistoryRepository;
import com.cartethyia.easyorange.admin.util.BatchQueryUtil;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminReportService 单元测试")
class AdminReportServiceTest {

    @Mock
    private ProductReportRepository productReportRepository;

    @Mock
    private ReportHandleHistoryRepository reportHandleHistoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCachePort<?> productCachePort;

    @Mock
    private BatchQueryUtil batchQueryUtil;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private AdminReportService reportService;

    private static final Long REPORT_ID = 100L;
    private static final Long PRODUCT_ID = 200L;
    private static final Long REPORTER_ID = 1L;
    private static final Long OPERATOR_ID = 2L;

    private ProductReport createPendingReport() {
        return ProductReport.reconstitute(REPORT_ID, PRODUCT_ID, REPORTER_ID,
                "虚假信息", ProductReportStatus.PENDING, null,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().minusHours(1), 1);
    }

    private ProductReport createResolvedReport() {
        return ProductReport.reconstitute(REPORT_ID, PRODUCT_ID, REPORTER_ID,
                "虚假信息", ProductReportStatus.RESOLVED, "已处理",
                LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), 1);
    }

    private UserEntity createUser(Long id, String name) {
        return UserEntity.builder()
                .id(id)
                .username(name)
                .nickName(name)
                .userType(UserType.fromCode("01"))
                .status(UserStatus.NORMAL)
                .build();
    }

    @Nested
    @DisplayName("listReports")
    class ListReportsTests {

        @Test
        @DisplayName("分页查询举报列表")
        void listReports_withStatus_returnsPage() {
            ProductReport report = createPendingReport();
            PageResult<ProductReport> pageResult = PageResult.of(List.of(report), 1L, 1, 20);
            when(productReportRepository.findByStatus(0, 1, 20)).thenReturn(pageResult);
            when(batchQueryUtil.batchGetUsers(anyList())).thenReturn(Map.of(REPORTER_ID, createUser(REPORTER_ID, "举报人")));
            ProductDO reportTestProduct = ProductDO.builder().id(PRODUCT_ID).name("测试商品").price(new BigDecimal("99.99")).build();
            reportTestProduct.setDelFlag(0);
            when(batchQueryUtil.batchGetProducts(anyList())).thenReturn(Map.of(PRODUCT_ID, reportTestProduct));

            PageResult<AdminReportResponse> result = reportService.listReports(1, 20, 0);

            assertThat(result.records()).hasSize(1);
            AdminReportResponse vo = result.records().get(0);
            assertThat(vo.reportId()).isEqualTo(REPORT_ID);
            assertThat(vo.productName()).isEqualTo("测试商品");
            assertThat(vo.reporterName()).isEqualTo("举报人");
            assertThat(vo.status()).isEqualTo(0);
            assertThat(vo.statusDesc()).isEqualTo("待处理");
        }
    }

    @Nested
    @DisplayName("getReportDetail")
    class GetReportDetailTests {

        @Test
        @DisplayName("获取举报详情成功")
        void getReportDetail_success() {
            ProductReport report = createPendingReport();
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report);
            when(batchQueryUtil.batchGetUsers(anyList())).thenReturn(Map.of(REPORTER_ID, createUser(REPORTER_ID, "举报人")));
            ProductDO testProduct2 = ProductDO.builder().id(PRODUCT_ID).name("测试商品").price(new BigDecimal("99.99")).build();
            testProduct2.setDelFlag(0);
            when(batchQueryUtil.batchGetProducts(anyList())).thenReturn(Map.of(PRODUCT_ID, testProduct2));

            AdminReportResponse vo = reportService.getReportDetail(REPORT_ID);

            assertThat(vo).isNotNull();
            assertThat(vo.reportId()).isEqualTo(REPORT_ID);
        }

        @Test
        @DisplayName("举报不存在时抛出异常")
        void getReportDetail_notFound_throws() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(null);

            assertThatThrownBy(() -> reportService.getReportDetail(REPORT_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("举报记录不存在");
        }
    }

    @Nested
    @DisplayName("handleReport")
    class HandleReportTests {

        @Test
        @DisplayName("处理举报 — resolve 动作")
        void handleReport_resolve_success() {
            ProductReport report = createPendingReport();
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report);

            ReportHandleRequest request = new ReportHandleRequest();
            request.setAction("resolve");
            request.setRemark("已核实处理");

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {

                reportService.handleReport(REPORT_ID, request);

                verify(productReportRepository).update(argThat(r -> r != null && !r.isPending()));
                verify(reportHandleHistoryRepository).save(any(ReportHandleHistory.class));
                verify(domainEventPublisher).publish(any(BaseDomainEvent.class));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("处理举报 — dismiss 动作")
        void handleReport_dismiss_success() {
            ProductReport report = createPendingReport();
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report);

            ReportHandleRequest request = new ReportHandleRequest();
            request.setAction("dismiss");

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {

                reportService.handleReport(REPORT_ID, request);

                verify(productReportRepository).update(argThat(r -> r != null && !r.isPending()));
                verify(reportHandleHistoryRepository).save(any(ReportHandleHistory.class));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("处理已处理过的举报抛出异常")
        void handleReport_alreadyHandled_throws() {
            ProductReport report = createResolvedReport();
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report);

            ReportHandleRequest request = new ReportHandleRequest();
            request.setAction("resolve");

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {

                assertThatThrownBy(() -> reportService.handleReport(REPORT_ID, request))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("已被处理");
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("处理不存在举报抛出异常")
        void handleReport_notFound_throws() {
            when(productReportRepository.findById(REPORT_ID)).thenReturn(null);

            ReportHandleRequest request = new ReportHandleRequest();
            request.setAction("resolve");

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {

                assertThatThrownBy(() -> reportService.handleReport(REPORT_ID, request))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("举报记录不存在");
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("PRODUCT_OFFLINE 动作下架商品")
        void handleReport_productOffline_takesProductOffline() {
            ProductReport report = createPendingReport();
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report);

            Product product = Product.reconstitute(
                    ProductId.of(PRODUCT_ID), SellerId.of(1L), CategoryId.of(1L),
                    ProductTitle.of("测试商品"), Money.of(new BigDecimal("99.99")), null,
                    null, ConsignmentMode.MANUAL, null, 0,
                    StockQuantity.of(10), Version.INITIAL, ProductStatus.ONLINE,
                    0, null, null, null, null, null, TagSet.empty(), null,
                    LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
            );
            when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                    .thenReturn(Optional.of(product));

            ReportHandleRequest request = new ReportHandleRequest();
            request.setAction("PRODUCT_OFFLINE");

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {

                reportService.handleReport(REPORT_ID, request);

                verify(productRepository).update(argThat(p -> p.getStatus() == ProductStatus.OFFLINE));
                verify(productCachePort).evictProductCache(PRODUCT_ID);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("batchHandleReports")
    class BatchHandleReportsTests {

        @Test
        @DisplayName("批量处理举报")
        void batchHandleReports_success() {
            ProductReport report1 = createPendingReport();
            ProductReport report2 = ProductReport.reconstitute(101L, PRODUCT_ID, REPORTER_ID,
                    "侵权", ProductReportStatus.PENDING, null,
                    LocalDateTime.now().minusHours(1), LocalDateTime.now().minusHours(1), 2);

            when(productReportRepository.findById(100L)).thenReturn(report1);
            when(productReportRepository.findById(101L)).thenReturn(report2);

            BatchHandleRequest request = new BatchHandleRequest();
            request.setReportIds(List.of(100L, 101L));
            request.setAction("dismiss");

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {

                reportService.batchHandleReports(request);

                verify(productReportRepository, times(2)).update(any(ProductReport.class));
                verify(reportHandleHistoryRepository, times(2)).save(any(ReportHandleHistory.class));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("空列表抛出异常")
        void batchHandleReports_emptyList_throws() {
            BatchHandleRequest request = new BatchHandleRequest();
            request.setReportIds(List.of());
            request.setAction("dismiss");

            assertThatThrownBy(() -> reportService.batchHandleReports(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能为空");
        }

        @Test
        @DisplayName("超过50条抛出异常")
        void batchHandleReports_exceedLimit_throws() {
            List<Long> ids = java.util.stream.LongStream.range(1, 52).boxed().toList();
            BatchHandleRequest request = new BatchHandleRequest();
            request.setReportIds(ids);
            request.setAction("dismiss");

            assertThatThrownBy(() -> reportService.batchHandleReports(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能超过50条");
        }
    }

    @Nested
    @DisplayName("getReportStats / getReportHistory")
    class StatsAndHistoryTests {

        @Test
        @DisplayName("获取举报统计")
        void getReportStats_returnsStats() {
            when(productReportRepository.countByStatus(null)).thenReturn(10L);
            when(productReportRepository.countByStatus(0)).thenReturn(5L);
            when(productReportRepository.countByStatus(1)).thenReturn(2L);
            when(productReportRepository.countByStatus(2)).thenReturn(2L);
            when(productReportRepository.countByStatus(3)).thenReturn(1L);

            ReportStatsResponse stats = reportService.getReportStats();

            assertThat(stats.totalReports()).isEqualTo(10);
            assertThat(stats.pendingReports()).isEqualTo(5);
            assertThat(stats.resolvedReports()).isEqualTo(2);
            assertThat(stats.dismissedReports()).isEqualTo(1);
        }

        @Test
        @DisplayName("获取举报处理历史")
        void getReportHistory_returnsHistory() {
            ReportHandleHistory history = ReportHandleHistory.reconstitute(1L, REPORT_ID, OPERATOR_ID,
                    "resolve", "已处理", LocalDateTime.now());

            when(reportHandleHistoryRepository.findByReportId(REPORT_ID)).thenReturn(List.of(history));
            when(batchQueryUtil.batchGetUsers(anyList())).thenReturn(Map.of(OPERATOR_ID, createUser(OPERATOR_ID, "管理员")));

            List<ReportHandleHistoryResponse> historyList = reportService.getReportHistory(REPORT_ID);

            assertThat(historyList).hasSize(1);
            assertThat(historyList.get(0).action()).isEqualTo("resolve");
            assertThat(historyList.get(0).actionDesc()).isEqualTo("处理通过");
        }
    }
}
