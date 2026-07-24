package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductReportQueryRepository;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportDetailResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReportQueryService 测试")
class ProductReportQueryServiceTest {

    @Mock
    private ProductReportRepository productReportRepository;

    @Mock
    private ProductReportQueryRepository productReportQueryRepository;

    @Mock
    private ProductQueryRepository productQueryRepository;

    private ProductReportQueryService service;

    @BeforeEach
    void setUp() {
        service = new ProductReportQueryService(productReportRepository, productReportQueryRepository, productQueryRepository);
    }

    @Nested
    @DisplayName("getMyReports")
    class GetMyReports {

        @Test
        @DisplayName("应返回分页结果")
        void shouldReturnPaginatedReports() {
            ProductReport report1 = ProductReport.create("1", "2", "假货", "1");
            report1 = report1.assignId("100");
            ProductReport report2 = ProductReport.create("1", "3", "侵权", "2");
            report2 = report2.assignId("101");

            List<ProductReport> reports = List.of(report1, report2);
            PageResult<ProductReport> pageResult = PageResult.of(reports, 2L, 1, 20);
            when(productReportQueryRepository.findByReporterId("2", 1, 20)).thenReturn(pageResult);

            PageResult<ProductReportResponse> result = service.getMyReports("2", 1, 20);

            assertThat(result).isNotNull();
            assertThat(result.records()).hasSize(2);
            assertThat(result.total()).isEqualTo(2L);
            assertThat(result.records().get(0).id()).isEqualTo("100");
            assertThat(result.records().get(0).productId()).isEqualTo("1");
            assertThat(result.records().get(0).reason()).isEqualTo("假货");
            assertThat(result.records().get(0).status()).isEqualTo("0");
        }

        @Test
        @DisplayName("结果为空时应返回空分页")
        void withEmptyResult_shouldReturnEmptyPage() {
            when(productReportQueryRepository.findByReporterId("2", 1, 20))
                    .thenReturn(PageResult.of(List.of(), 0L, 1, 20));

            PageResult<ProductReportResponse> result = service.getMyReports("2", 1, 20);

            assertThat(result.records()).isEmpty();
            assertThat(result.total()).isZero();
        }

        @Test
        @DisplayName("null 的记录应返回 null")
        void withNullReport_shouldReturnNull() {
            ProductReport report = ProductReport.create("1", "2", "假货", "1");
            report = report.assignId("100");

            List<ProductReport> reports = Arrays.asList(report, null);
            when(productReportQueryRepository.findByReporterId("2", 1, 20))
                    .thenReturn(PageResult.of(reports, 2L, 1, 20));

            PageResult<ProductReportResponse> result = service.getMyReports("2", 1, 20);

            assertThat(result.records()).hasSize(2);
            assertThat(result.records().get(0)).isNotNull();
            assertThat(result.records().get(1)).isNull();
        }
    }

    @Nested
    @DisplayName("getReportDetail")
    class GetReportDetail {

        private ProductReport report;
        private static final String REPORTER_ID = "2";
        private static final String PRODUCT_ID = "1";

        @BeforeEach
        void setUp() {
            report = ProductReport.create(PRODUCT_ID, REPORTER_ID, "假货", "1");
            report = report.assignId("100");
        }

        @Test
        @DisplayName("应返回完整详情")
        void shouldReturnDetail() {
            when(productReportRepository.findById("100")).thenReturn(report);
            ProductReadModel product = createProductReadModel(PRODUCT_ID, "测试商品");
            when(productQueryRepository.findProductById(PRODUCT_ID)).thenReturn(product);

            ProductReportDetailResponse response = service.getReportDetail("100", REPORTER_ID);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo("100");
            assertThat(response.productId()).isEqualTo(PRODUCT_ID);
            assertThat(response.productName()).isEqualTo("测试商品");
            assertThat(response.reason()).isEqualTo("假货");
            assertThat(response.reasonType()).isEqualTo("1");
            assertThat(response.status()).isEqualTo("0");
            assertThat(response.statusDesc()).isEqualTo("待处理");
        }

        @Test
        @DisplayName("不存在的举报应抛出异常")
        void whenNotFound_shouldThrow() {
            when(productReportRepository.findById("999")).thenReturn(null);

            assertThatThrownBy(() -> service.getReportDetail("999", REPORTER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("举报记录不存在");
        }

        @Test
        @DisplayName("非举报人查询应抛出异常")
        void whenNotOwner_shouldThrow() {
            when(productReportRepository.findById("100")).thenReturn(report);

            assertThatThrownBy(() -> service.getReportDetail("100", "999"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权查看");
        }

        @Test
        @DisplayName("商品不存在时 productName 应为 null")
        void whenProductNotFound_shouldReturnNullProductName() {
            when(productReportRepository.findById("100")).thenReturn(report);
            when(productQueryRepository.findProductById(PRODUCT_ID)).thenReturn(null);

            ProductReportDetailResponse response = service.getReportDetail("100", REPORTER_ID);

            assertThat(response.productName()).isNull();
        }

        @Test
        @DisplayName("已处理的举报应显示正确的状态描述")
        void shouldReturnCorrectStatusDesc() {
            report = report.reject("证据不足");
            when(productReportRepository.findById("100")).thenReturn(report);

            ProductReportDetailResponse response = service.getReportDetail("100", REPORTER_ID);

            assertThat(response.status()).isEqualTo("3");
            assertThat(response.statusDesc()).isEqualTo("已驳回");
        }

        private ProductReadModel createProductReadModel(String id, String title) {
            return new ProductReadModel(
                    id, "seller1", "seller", null, "cat1", "分类",
                    title, "描述", null, null, 10,
                    "0", "草稿", 0, null, null, null, null,
                    List.of(), "main.jpg", LocalDateTime.now(), LocalDateTime.now()
            );
        }
    }
}
