package com.cartethyia.easyorange.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminReportAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchHandleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ReportHandleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminReportResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchHandleResultResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ReportHandleHistoryResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ReportStatsResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.product.application.port.query.ProductReportQueryRepository;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.entity.ReportHandleHistory;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.event.ReportProcessedEvent;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.repository.ReportHandleHistoryRepository;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TagSet;
import com.cartethyia.easyorange.product.domain.valueobject.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminReportService 单元测试")
class AdminReportServiceTest {

    @Mock
    private ProductReportRepository productReportRepository;

    @Mock
    private ProductReportQueryRepository productReportQueryRepository;

    @Mock
    private ReportHandleHistoryRepository reportHandleHistoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCacheEvictionPort productCachePort;

    @Mock
    private AdminUserQueryPort adminUserQueryPort;

    @Mock
    private AdminProductQueryPort adminProductQueryPort;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Spy
    private AdminReportAssembler assembler = new AdminReportAssembler();

    @InjectMocks
    private AdminReportService reportService;

    private static final String REPORT_ID = "100";
    private static final String PRODUCT_ID = "200";
    private static final String REPORTER_ID = "1";
    private static final String OPERATOR_ID = "2";

    private ProductReport createPendingReport() {
        return ProductReport.reconstitute(
                REPORT_ID,
                PRODUCT_ID,
                REPORTER_ID,
                "虚假信息",
                ProductReportStatus.PENDING,
                null,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusHours(1),
                "1");
    }

    private ProductReport createResolvedReport() {
        return ProductReport.reconstitute(
                REPORT_ID,
                PRODUCT_ID,
                REPORTER_ID,
                "虚假信息",
                ProductReportStatus.RESOLVED,
                "已处理",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                "1");
    }

    private AdminUserQueryPort.UserInfo createUser(String id, String name) {
        return new AdminUserQueryPort.UserInfo(id, name, name, null, null);
    }

    private Product createOnlineProduct() {
        return Product.builder()
                .id(ProductId.of(PRODUCT_ID))
                .sellerId(SellerId.of("1"))
                .categoryId(CategoryId.of("1"))
                .title(ProductTitle.of("测试商品"))
                .price(Money.of(new BigDecimal("99.99")))
                .stock(StockQuantity.of(10))
                .version(Version.INITIAL)
                .status(ProductStatus.ONLINE)
                .tags(TagSet.empty())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
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
            when(productReportQueryRepository.findByStatus("0", 1, 20)).thenReturn(pageResult);
            when(adminUserQueryPort.getUserInfos(anyList()))
                    .thenReturn(Map.of(REPORTER_ID, createUser(REPORTER_ID, "举报人")));
            when(adminProductQueryPort.getProductInfos(anyList()))
                    .thenReturn(Map.of(PRODUCT_ID, new AdminProductQueryPort.ProductInfo(PRODUCT_ID, "测试商品")));

            PageResult<AdminReportResponse> result = reportService.listReports(1, 20, 0);

            assertThat(result.records()).hasSize(1);
            AdminReportResponse vo = result.records().get(0);
            assertThat(vo.reportId()).isEqualTo(REPORT_ID);
            assertThat(vo.productName()).isEqualTo("测试商品");
            assertThat(vo.reporterName()).isEqualTo("举报人");
            assertThat(vo.status()).isEqualTo(0);
            assertThat(vo.statusDesc()).isEqualTo("待处理");
        }

        @Test
        @DisplayName("无状态筛选时不传 status 给查询仓储")
        void listReports_withoutStatus_passesNull() {
            ProductReport report = createPendingReport();
            PageResult<ProductReport> pageResult = PageResult.of(List.of(report), 1L, 1, 20);
            when(productReportQueryRepository.findByStatus(null, 1, 20)).thenReturn(pageResult);
            when(adminUserQueryPort.getUserInfos(anyList())).thenReturn(Map.of());
            when(adminProductQueryPort.getProductInfos(anyList())).thenReturn(Map.of());

            PageResult<AdminReportResponse> result = reportService.listReports(1, 20, null);

            assertThat(result.records()).hasSize(1);
            verify(productReportQueryRepository).findByStatus(null, 1, 20);
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
            when(adminUserQueryPort.getUserInfos(anyList()))
                    .thenReturn(Map.of(REPORTER_ID, createUser(REPORTER_ID, "举报人")));
            when(adminProductQueryPort.getProductInfos(anyList()))
                    .thenReturn(Map.of(PRODUCT_ID, new AdminProductQueryPort.ProductInfo(PRODUCT_ID, "测试商品")));

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
                verify(domainEventPublisher).publish(any(ReportProcessedEvent.class));
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
            when(productRepository.findById(ProductId.of(PRODUCT_ID))).thenReturn(Optional.of(createOnlineProduct()));

            ReportHandleRequest request = new ReportHandleRequest();
            request.setAction("PRODUCT_OFFLINE");

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {

                reportService.handleReport(REPORT_ID, request);

                verify(productRepository).save(argThat(p -> p.getStatus() == ProductStatus.OFFLINE));
                verify(productCachePort).evictProductCache(PRODUCT_ID);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("PRODUCT_OFFLINE 动作商品不存在时抛出异常")
        void handleReport_productOffline_missingProduct_throws() {
            ProductReport report = createPendingReport();
            when(productReportRepository.findById(REPORT_ID)).thenReturn(report);
            when(productRepository.findById(ProductId.of(PRODUCT_ID))).thenReturn(Optional.empty());

            ReportHandleRequest request = new ReportHandleRequest();
            request.setAction("PRODUCT_OFFLINE");

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {

                assertThatThrownBy(() -> reportService.handleReport(REPORT_ID, request))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("关联商品不存在");
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("batchHandleReports")
    class BatchHandleReportsTests {

        @Test
        @DisplayName("批量处理举报返回聚合结果")
        void batchHandleReports_success() {
            ProductReport report1 = createPendingReport();
            ProductReport report2 = ProductReport.reconstitute(
                    "101",
                    PRODUCT_ID,
                    REPORTER_ID,
                    "侵权",
                    ProductReportStatus.PENDING,
                    null,
                    LocalDateTime.now().minusHours(1),
                    LocalDateTime.now().minusHours(1),
                    "2");

            when(productReportRepository.findById("100")).thenReturn(report1);
            when(productReportRepository.findById("101")).thenReturn(report2);

            BatchHandleRequest request = new BatchHandleRequest();
            request.setReportIds(List.of("100", "101"));
            request.setAction("dismiss");

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {

                BatchHandleResultResponse result = reportService.batchHandleReports(request);

                assertThat(result.total()).isEqualTo(2);
                assertThat(result.success()).isEqualTo(2);
                assertThat(result.failed()).isZero();
                assertThat(result.errors()).isEmpty();
                verify(productReportRepository, times(2)).update(any(ProductReport.class));
                verify(reportHandleHistoryRepository, times(2)).save(any(ReportHandleHistory.class));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("批量处理部分失败时聚合错误信息")
        void batchHandleReports_partialFailure_aggregatesErrors() {
            ProductReport pending = createPendingReport();
            ProductReport resolved = ProductReport.reconstitute(
                    "101",
                    PRODUCT_ID,
                    REPORTER_ID,
                    "侵权",
                    ProductReportStatus.RESOLVED,
                    "已处理",
                    LocalDateTime.now().minusHours(2),
                    LocalDateTime.now().minusHours(1),
                    "2");

            when(productReportRepository.findById("100")).thenReturn(pending);
            when(productReportRepository.findById("101")).thenReturn(resolved);

            BatchHandleRequest request = new BatchHandleRequest();
            request.setReportIds(List.of("100", "101"));
            request.setAction("dismiss");

            TestSecurityUtil.setSecurityContext(OPERATOR_ID);
            try {

                BatchHandleResultResponse result = reportService.batchHandleReports(request);

                assertThat(result.total()).isEqualTo(2);
                assertThat(result.success()).isEqualTo(1);
                assertThat(result.failed()).isEqualTo(1);
                assertThat(result.errors()).hasSize(1);
                assertThat(result.errors().get(0)).contains("101");
                verify(productReportRepository, times(1)).update(any(ProductReport.class));
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
            List<String> ids = java.util.stream.LongStream.range(1, 52)
                    .mapToObj(String::valueOf)
                    .toList();
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
            when(productReportQueryRepository.countByStatus(null)).thenReturn(10L);
            when(productReportQueryRepository.countByStatus("0")).thenReturn(5L);
            when(productReportQueryRepository.countByStatus("1")).thenReturn(2L);
            when(productReportQueryRepository.countByStatus("2")).thenReturn(2L);
            when(productReportQueryRepository.countByStatus("3")).thenReturn(1L);

            ReportStatsResponse stats = reportService.getReportStats();

            assertThat(stats.totalReports()).isEqualTo(10);
            assertThat(stats.pendingReports()).isEqualTo(5);
            assertThat(stats.resolvedReports()).isEqualTo(2);
            assertThat(stats.dismissedReports()).isEqualTo(1);
        }

        @Test
        @DisplayName("获取举报处理历史")
        void getReportHistory_returnsHistory() {
            ReportHandleHistory history = ReportHandleHistory.reconstitute(
                    "1", REPORT_ID, OPERATOR_ID, "resolve", "已处理", LocalDateTime.now());

            when(reportHandleHistoryRepository.findByReportId(REPORT_ID)).thenReturn(List.of(history));
            when(adminUserQueryPort.getUserInfos(anyList()))
                    .thenReturn(Map.of(OPERATOR_ID, createUser(OPERATOR_ID, "管理员")));

            List<ReportHandleHistoryResponse> historyList = reportService.getReportHistory(REPORT_ID);

            assertThat(historyList).hasSize(1);
            assertThat(historyList.get(0).action()).isEqualTo("resolve");
            assertThat(historyList.get(0).actionDesc()).isEqualTo("处理通过");
        }
    }
}
