package com.cartethyia.easyorange.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminProductAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminProductQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminProductResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductSummary;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductService 单元测试")
class AdminProductServiceTest {

    @Mock
    private AdminProductQueryPort adminProductQueryPort;

    @Spy
    private AdminProductAssembler adminProductAssembler = new AdminProductAssembler();

    @InjectMocks
    private AdminProductService productService;

    private static final String PRODUCT_ID = "100";
    private static final String SELLER_ID = "1";

    private ProductSummary createProductSummary(String status) {
        return new ProductSummary(
                PRODUCT_ID,
                "测试商品",
                new BigDecimal("99.99"),
                new BigDecimal("199.99"),
                10,
                status,
                describeStatus(status),
                "1",
                "北京",
                "微信",
                "1",
                SELLER_ID,
                10,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private ProductDetail createProductDetail(String status) {
        return new ProductDetail(
                PRODUCT_ID,
                "测试商品",
                "商品描述",
                new BigDecimal("99.99"),
                new BigDecimal("199.99"),
                10,
                status,
                describeStatus(status),
                "1",
                "北京",
                "微信",
                "1",
                SELLER_ID,
                10,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private String describeStatus(String code) {
        return switch (code) {
            case "1" -> "草稿";
            case "2" -> "待审核";
            case "3" -> "已驳回";
            case "4" -> "上架";
            case "5" -> "下架";
            case "6" -> "已售出";
            case null -> "未知状态";
            default -> "未知状态";
        };
    }

    @Nested
    @DisplayName("listProducts")
    class ListProductsTests {

        @Test
        @DisplayName("分页查询商品列表")
        void listProducts_returnsPage() {
            AdminProductQueryRequest request =
                    new AdminProductQueryRequest(null, null, null, null, null, null, null, null);
            ProductSummary product = createProductSummary("1");

            when(adminProductQueryPort.queryProducts(any(ProductQueryCondition.class)))
                    .thenReturn(new ProductQueryResult(List.of(product), 1, 1, 20));
            when(adminProductQueryPort.getProductImages(anyList())).thenReturn(Map.of());

            PageResult<AdminProductResponse> result = productService.listProducts(request);

            assertThat(result.records()).hasSize(1);
            assertThat(result.records().get(0).name()).isEqualTo("测试商品");
            assertThat(result.total()).isEqualTo(1);
        }

        @Test
        @DisplayName("带关键词和状态过滤")
        void listProducts_withFilters_returnsFiltered() {
            AdminProductQueryRequest request =
                    new AdminProductQueryRequest(null, null, "测试", null, "4", SELLER_ID, null, null);
            ProductSummary product = createProductSummary("4");

            when(adminProductQueryPort.queryProducts(any(ProductQueryCondition.class)))
                    .thenReturn(new ProductQueryResult(List.of(product), 1, 1, 20));
            when(adminProductQueryPort.getProductImages(anyList())).thenReturn(Map.of());

            PageResult<AdminProductResponse> result = productService.listProducts(request);

            assertThat(result.records()).hasSize(1);
            assertThat(result.records().get(0).name()).isEqualTo("测试商品");
        }
    }

    @Nested
    @DisplayName("getProductDetail")
    class GetProductDetailTests {

        @Test
        @DisplayName("获取商品详情成功")
        void getProductDetail_success() {
            ProductDetail detail = createProductDetail("1");
            when(adminProductQueryPort.getProductDetail(PRODUCT_ID)).thenReturn(detail);
            when(adminProductQueryPort.getProductImages(anyList())).thenReturn(Map.of(PRODUCT_ID, List.of("img.jpg")));

            AdminProductResponse vo = productService.getProductDetail(PRODUCT_ID);

            assertThat(vo).isNotNull();
            assertThat(vo.productId()).isEqualTo(PRODUCT_ID);
            assertThat(vo.name()).isEqualTo("测试商品");
            assertThat(vo.description()).isEqualTo("商品描述");
            assertThat(vo.images()).contains("img.jpg");
        }

        @Test
        @DisplayName("商品不存在时抛出异常")
        void getProductDetail_notFound_throws() {
            when(adminProductQueryPort.getProductDetail(PRODUCT_ID)).thenReturn(null);

            assertThatThrownBy(() -> productService.getProductDetail(PRODUCT_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("商品不存在");
        }
    }

    @Nested
    @DisplayName("updateProductStatus")
    class UpdateProductStatusTests {

        @Test
        @DisplayName("更新商品状态委托端口")
        void updateProductStatus_success() {
            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus("OFFLINE");

            productService.updateProductStatus(PRODUCT_ID, request);

            verify(adminProductQueryPort).applyProductStatus(PRODUCT_ID, "OFFLINE");
        }
    }
}
