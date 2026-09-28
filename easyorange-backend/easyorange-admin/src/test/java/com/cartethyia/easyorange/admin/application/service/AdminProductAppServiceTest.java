package com.cartethyia.easyorange.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.admin.domain.exception.AdminDomainException;
import com.cartethyia.easyorange.admin.domain.model.ProductDetailView;
import com.cartethyia.easyorange.admin.domain.model.ProductListView;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductSummary;
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
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 商品服务测试 — 断言端口编排结果（分页原样返回 + 图片按当页 id 批量取回），
 * 商品 → 响应 DTO 的字段命名由 {@code AdminProductAssembler} 负责，不在这里复测一遍。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductAppService 单元测试")
class AdminProductAppServiceTest {

    @Mock
    private AdminProductPort adminProductPort;

    @InjectMocks
    private AdminProductAppService productService;

    private static final String PRODUCT_ID = "100";
    private static final String SELLER_ID = "1";

    private static final ProductQueryCondition CONDITION =
            new ProductQueryCondition(null, null, null, null, null, null, 1, 20);

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
        @DisplayName("分页查询商品列表并按当页 id 取回图片")
        void listProducts_returnsPageWithImages() {
            ProductSummary product = createProductSummary("1");
            when(adminProductPort.queryProducts(CONDITION))
                    .thenReturn(new ProductQueryResult(List.of(product), 1, 1, 20));
            when(adminProductPort.getProductImages(List.of(PRODUCT_ID)))
                    .thenReturn(Map.of(PRODUCT_ID, List.of("img.jpg")));

            ProductListView view = productService.listProducts(CONDITION);

            assertThat(view.page().total()).isEqualTo(1);
            assertThat(view.page().records().get(0).name()).isEqualTo("测试商品");
            assertThat(view.images()).containsKey(PRODUCT_ID);
        }

        @Test
        @DisplayName("查询条件原样下发端口")
        void listProducts_passesConditionToPort() {
            ProductQueryCondition condition = new ProductQueryCondition("测试", null, "4", SELLER_ID, null, null, 1, 20);
            when(adminProductPort.queryProducts(condition))
                    .thenReturn(new ProductQueryResult(List.of(createProductSummary("4")), 1, 1, 20));
            when(adminProductPort.getProductImages(anyList())).thenReturn(Map.of());

            ProductListView view = productService.listProducts(condition);

            assertThat(view.page().records()).hasSize(1);
            verify(adminProductPort).queryProducts(condition);
        }
    }

    @Nested
    @DisplayName("getProductDetail")
    class GetProductDetailTests {

        @Test
        @DisplayName("获取商品详情成功并带上图片")
        void getProductDetail_success() {
            when(adminProductPort.getProductDetail(PRODUCT_ID)).thenReturn(createProductDetail("1"));
            when(adminProductPort.getProductImages(List.of(PRODUCT_ID)))
                    .thenReturn(Map.of(PRODUCT_ID, List.of("img.jpg")));

            ProductDetailView view = productService.getProductDetail(PRODUCT_ID);

            assertThat(view.product().id()).isEqualTo(PRODUCT_ID);
            assertThat(view.product().name()).isEqualTo("测试商品");
            assertThat(view.images()).containsExactly("img.jpg");
        }

        @Test
        @DisplayName("商品不存在时抛出异常")
        void getProductDetail_notFound_throws() {
            when(adminProductPort.getProductDetail(PRODUCT_ID)).thenReturn(null);

            assertThatThrownBy(() -> productService.getProductDetail(PRODUCT_ID))
                    .isInstanceOf(AdminDomainException.class)
                    .hasMessageContaining("商品不存在");
        }
    }

    @Nested
    @DisplayName("updateProductStatus")
    class UpdateProductStatusTests {

        @Test
        @DisplayName("更新商品状态委托端口")
        void updateProductStatus_success() {
            productService.updateProductStatus(PRODUCT_ID, "OFFLINE");

            verify(adminProductPort).applyProductStatus(PRODUCT_ID, "OFFLINE");
        }
    }
}
