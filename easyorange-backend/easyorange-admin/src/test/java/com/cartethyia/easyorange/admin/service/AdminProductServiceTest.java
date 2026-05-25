package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.admin.dto.request.AdminProductQueryRequest;
import com.cartethyia.easyorange.admin.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.dto.response.AdminProductResponse;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductImageMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductService 单元测试")
class AdminProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductDetailMapper productDetailMapper;

    @Mock
    private ProductImageMapper productImageMapper;

    @InjectMocks
    private AdminProductService productService;

    private static final Long PRODUCT_ID = 100L;
    private static final Long SELLER_ID = 1L;

    private ProductDO createProduct(int status) {
        ProductDO product = ProductDO.builder()
                .id(PRODUCT_ID)
                .userId(SELLER_ID)
                .name("测试商品")
                .price(new BigDecimal("99.99"))
                .status(status)
                .viewCount(10)
                .build();
        product.setDelFlag(0);
        return product;
    }

    @Nested
    @DisplayName("listProducts")
    class ListProductsTests {

        @Test
        @DisplayName("分页查询商品列表")
        void listProducts_returnsPage() {
            AdminProductQueryRequest request = new AdminProductQueryRequest(null, null, null, null, null, null, null, null);
            ProductDO product = createProduct(1);

            when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(invocation -> {
                        Page<ProductDO> p = invocation.getArgument(0);
                        p.setRecords(List.of(product));
                        p.setTotal(1);
                        return p;
                    });
            when(productImageMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            PageResult<AdminProductResponse> result = productService.listProducts(request);

            assertThat(result.records()).hasSize(1);
            assertThat(result.records().get(0).name()).isEqualTo("测试商品");
            assertThat(result.total()).isEqualTo(1);
        }

        @Test
        @DisplayName("带关键词和状态过滤")
        void listProducts_withFilters_returnsFiltered() {
            AdminProductQueryRequest request = new AdminProductQueryRequest(null, null, "测试", null, 4, SELLER_ID, null, null);

            ProductDO product = createProduct(4);

            when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(invocation -> {
                        Page<ProductDO> p = invocation.getArgument(0);
                        p.setRecords(List.of(product));
                        p.setTotal(1);
                        return p;
                    });
            when(productImageMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

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
            ProductDO product = createProduct(1);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
            when(productImageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
                    List.of(ProductImageDO.builder().productId(PRODUCT_ID).imageUrl("img.jpg").build())
            );
            when(productDetailMapper.selectDetailsByProductIds(anyList()))
                    .thenReturn(List.of(ProductDetailDO.builder().description("商品描述").build()));

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
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(null);

            assertThatThrownBy(() -> productService.getProductDetail(PRODUCT_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("商品不存在");
        }
    }

    @Nested
    @DisplayName("updateProductStatus")
    class UpdateProductStatusTests {

        @Test
        @DisplayName("更新商品状态成功")
        void updateProductStatus_success() {
            ProductDO product = createProduct(1);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus(3);

            productService.updateProductStatus(PRODUCT_ID, request);

            assertThat(product.getStatus()).isEqualTo(3);
            verify(productMapper).updateById(product);
        }

        @Test
        @DisplayName("不存在的商品更新状态抛出异常")
        void updateProductStatus_notFound_throws() {
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(null);

            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus(3);

            assertThatThrownBy(() -> productService.updateProductStatus(PRODUCT_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("商品不存在");
        }

        @Test
        @DisplayName("无效状态编码抛出异常")
        void updateProductStatus_invalidStatus_throws() {
            ProductDO product = createProduct(1);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus(999);

            assertThatThrownBy(() -> productService.updateProductStatus(PRODUCT_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无效的商品状态");
        }
    }
}
