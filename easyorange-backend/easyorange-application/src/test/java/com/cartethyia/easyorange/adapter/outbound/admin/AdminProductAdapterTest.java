package com.cartethyia.easyorange.adapter.outbound.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
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
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
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
    private UserMapper userMapper;

    @Mock
    private CategoryMapper categoryMapper;

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
                userMapper,
                categoryMapper,
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
                ImageSet.of(List.of("http://img/1.jpg")),
                null));
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

    @Nested
    @DisplayName("queryProducts 的分类筛选")
    class QueryProductsCategory {

        private static final String ROOT_CATEGORY_ID = "018bcfe5-6800";

        @BeforeAll
        static void initTableInfo() {
            // 纯 Mockito 环境无 MyBatis-Plus 启动，LambdaQueryWrapper 需要手动初始化实体元数据缓存
            var assistant = new MybatisMapperBuilderAssistant(new MybatisConfiguration(), "");
            TableInfoHelper.initTableInfo(assistant, ProductDO.class);
        }

        @Test
        @DisplayName("按分类筛选时展开子树而非精确匹配一级分类")
        void expandsSubtree() {
            when(categoryMapper.selectSubtreeIds(ROOT_CATEGORY_ID))
                    .thenReturn(List.of(ROOT_CATEGORY_ID, "018bcfe5-7f70", "018bcfe5-8358"));
            when(productMapper.selectPage(any(), any())).thenReturn(new Page<>());

            adapter.queryProducts(condition(ROOT_CATEGORY_ID));

            // 走子树才能命中挂在二级分类上的商品；精确匹配一级分类会让审核页每个筛选项都是 0 条
            verify(categoryMapper).selectSubtreeIds(ROOT_CATEGORY_ID);
            verify(productMapper).selectPage(any(), any());
        }

        @Test
        @DisplayName("分类不存在时返回空页而不是拼出空 IN 列表")
        void unknownCategory_returnsEmptyPage() {
            when(categoryMapper.selectSubtreeIds("missing")).thenReturn(List.of());

            var result = adapter.queryProducts(condition("missing"));

            // 空 IN 列表会被 MySQL 拒绝，整条请求变 500
            assertThat(result.records()).isEmpty();
            assertThat(result.total()).isZero();
            verify(productMapper, never()).selectPage(any(), any());
        }

        @Test
        @DisplayName("不按分类筛选时不查子树")
        void noCategory_skipsSubtreeLookup() {
            when(productMapper.selectPage(any(), any())).thenReturn(new Page<>());

            adapter.queryProducts(condition(null));

            verify(categoryMapper, never()).selectSubtreeIds(any());
        }

        private AdminProductPort.ProductQueryCondition condition(String categoryId) {
            return new AdminProductPort.ProductQueryCondition(null, categoryId, null, null, null, null, 1, 20);
        }
    }
}
