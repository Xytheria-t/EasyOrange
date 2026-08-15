package com.cartethyia.easyorange.adapter.outbound.admin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.ProductCreateSpec;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductAdapter 单元测试")
class AdminProductAdapterTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductDetailMapper productDetailMapper;

    @Mock
    private ProductImageMapper productImageMapper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCacheEvictionPort productCacheEvictionPort;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private AdminProductAdapter adapter;

    private static final String PRODUCT_ID = "100";
    private static final String SELLER_ID = "1";

    @BeforeEach
    void setUp() {
        adapter = new AdminProductAdapter(
                productMapper,
                productDetailMapper,
                productImageMapper,
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
}
