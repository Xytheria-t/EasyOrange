package com.cartethyia.easyorange.admin.service;

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
import com.cartethyia.easyorange.product.domain.aggregate.Product;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductService 单元测试")
class AdminProductServiceTest {

    @Mock
    private AdminProductQueryPort adminProductQueryPort;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCachePort<?> productCachePort;

    @InjectMocks
    private AdminProductService productService;

    private static final Long PRODUCT_ID = 100L;
    private static final Long SELLER_ID = 1L;

    private ProductSummary createProductSummary(int status) {
        return new ProductSummary(
            PRODUCT_ID, "测试商品", new BigDecimal("99.99"), new BigDecimal("199.99"),
            10, status, ProductStatus.getDescByCode(status), 1, "北京", "微信",
            1L, SELLER_ID, 10, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private ProductDetail createProductDetail(int status) {
        return new ProductDetail(
            PRODUCT_ID, "测试商品", "商品描述", new BigDecimal("99.99"),
            new BigDecimal("199.99"), 10, status, ProductStatus.getDescByCode(status),
            1, "北京", "微信", 1L, SELLER_ID, 10,
            LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("listProducts")
    class ListProductsTests {

        @Test
        @DisplayName("分页查询商品列表")
        void listProducts_returnsPage() {
            AdminProductQueryRequest request = new AdminProductQueryRequest(null, null, null, null, null, null, null, null);
            ProductSummary product = createProductSummary(1);

            when(adminProductQueryPort.queryProducts(any(ProductQueryCondition.class)))
                    .thenReturn(new ProductQueryResult(List.of(product), 1, 1, 20));
            when(adminProductQueryPort.getProductImages(anyList()))
                    .thenReturn(Map.of());

            PageResult<AdminProductResponse> result = productService.listProducts(request);

            assertThat(result.records()).hasSize(1);
            assertThat(result.records().get(0).name()).isEqualTo("测试商品");
            assertThat(result.total()).isEqualTo(1);
        }

        @Test
        @DisplayName("带关键词和状态过滤")
        void listProducts_withFilters_returnsFiltered() {
            AdminProductQueryRequest request = new AdminProductQueryRequest(null, null, "测试", null, 4, SELLER_ID, null, null);
            ProductSummary product = createProductSummary(4);

            when(adminProductQueryPort.queryProducts(any(ProductQueryCondition.class)))
                    .thenReturn(new ProductQueryResult(List.of(product), 1, 1, 20));
            when(adminProductQueryPort.getProductImages(anyList()))
                    .thenReturn(Map.of());

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
            ProductDetail detail = createProductDetail(1);
            when(adminProductQueryPort.getProductDetail(PRODUCT_ID)).thenReturn(detail);
            when(adminProductQueryPort.getProductImages(anyList()))
                    .thenReturn(Map.of(PRODUCT_ID, List.of("img.jpg")));

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
        @DisplayName("更新商品状态成功 — ONLINE -> OFFLINE")
        void updateProductStatus_success() {
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

            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus(3);

            productService.updateProductStatus(PRODUCT_ID, request);

            verify(productRepository).update(argThat(p -> p.getStatus() == ProductStatus.OFFLINE));
            verify(productCachePort).evictProductCache(PRODUCT_ID);
        }

        @Test
        @DisplayName("不存在的商品更新状态抛出异常")
        void updateProductStatus_notFound_throws() {
            when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                    .thenReturn(Optional.empty());

            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus(3);

            assertThatThrownBy(() -> productService.updateProductStatus(PRODUCT_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("商品不存在");
        }

        @Test
        @DisplayName("无效状态编码抛出异常")
        void updateProductStatus_invalidStatus_throws() {
            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus(999);

            assertThatThrownBy(() -> productService.updateProductStatus(PRODUCT_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无效的商品状态");
        }
    }
}