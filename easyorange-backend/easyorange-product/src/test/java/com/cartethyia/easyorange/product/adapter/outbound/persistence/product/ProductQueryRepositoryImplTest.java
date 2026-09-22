package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.search.HotKeywordMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.search.SearchHistoryMapper;
import com.cartethyia.easyorange.product.application.port.cache.CategoryCachePort;
import com.cartethyia.easyorange.product.application.service.SearchHistoryBufferAppService;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductQueryRepositoryImpl 测试")
class ProductQueryRepositoryImplTest {

    /**
     * {@code ChainWrappers.lambdaQueryChain} 需要实体在 MyBatis-Plus 的 lambda 缓存里注册，
     * 否则连 wrapper 都建不起来（纯单测没有 MyBatis 启动流程，只能手动注册一次）。
     */
    @BeforeAll
    static void initMybatisLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProductDO.class);
    }

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductDetailMapper productDetailMapper;

    @Mock
    private ProductImageMapper productImageMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private SearchHistoryMapper searchHistoryMapper;

    @Mock
    private HotKeywordMapper hotKeywordMapper;

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private SearchHistoryBufferAppService searchHistoryBufferService;

    @Mock
    private CategoryCachePort categoryCachePort;

    private ProductQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ProductQueryRepositoryImpl(
                productMapper,
                productDetailMapper,
                productImageMapper,
                categoryMapper,
                searchHistoryMapper,
                hotKeywordMapper,
                redisTemplate,
                searchHistoryBufferService,
                categoryCachePort);
    }

    @Test
    @DisplayName("findProductById 带出成色描述与地区——AI 详情面靠这两个字段比成色、比地区")
    void findProductById_mapsConditionDescAndLocation() {
        when(productMapper.selectById("p1"))
                .thenReturn(ProductDO.builder()
                        .id("p1")
                        .name("九成新显卡")
                        .price(new BigDecimal("3400.00"))
                        .status(ProductStatus.ONLINE)
                        .conditionLevel(ConditionLevel.LIKE_NEW)
                        .location("浙江省杭州市西湖区文三路 100 号")
                        .build());

        var readModel = repository.findProductById("p1");

        assertThat(readModel.condition()).isEqualTo("2");
        assertThat(readModel.conditionDesc()).isEqualTo("几乎全新");
        assertThat(readModel.status()).isEqualTo("ONLINE");
        assertThat(readModel.statusDesc()).isEqualTo("上架");
        // 原始列值、不脱敏：脱敏是买家可见出口（详情 VO / AI 详情适配器）的职责
        assertThat(readModel.location()).isEqualTo("浙江省杭州市西湖区文三路 100 号");
    }

    @Test
    @DisplayName("成色未设置时 condition 与 conditionDesc 同为 null")
    void findProductById_nullConditionLevel() {
        when(productMapper.selectById("p2"))
                .thenReturn(ProductDO.builder()
                        .id("p2")
                        .status(ProductStatus.ONLINE)
                        .build());

        var readModel = repository.findProductById("p2");

        assertThat(readModel.condition()).isNull();
        assertThat(readModel.conditionDesc()).isNull();
    }

    @Test
    @DisplayName("查无此商品返回 null，由调用方决定是否 404")
    void findProductById_notFound() {
        when(productMapper.selectById("missing")).thenReturn(null);

        assertThat(repository.findProductById("missing")).isNull();
    }

    @Test
    @DisplayName("批量映射不 join 副表 / 类目表——补字段不能把列表路径拖成 N+1")
    void findProductsByIds_mapsRowsOnly() {
        when(productMapper.selectList(any()))
                .thenReturn(List.of(
                        ProductDO.builder()
                                .id("p1")
                                .status(ProductStatus.ONLINE)
                                .conditionLevel(ConditionLevel.NEW)
                                .build(),
                        ProductDO.builder()
                                .id("p2")
                                .status(ProductStatus.SOLD)
                                .conditionLevel(ConditionLevel.FAIR)
                                .build()));

        var readModels = repository.findProductsByIds(List.of("p1", "p2"));

        assertThat(readModels).extracting("conditionDesc").containsExactly("全新", "明显使用痕迹");
        verifyNoInteractions(productDetailMapper, productImageMapper, categoryMapper);
    }

    @Captor
    private ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper<ProductDO>> countWrapperCaptor;

    @Test
    @DisplayName("countByStatus(null) 不拼状态条件——总商品数不能因 status = NULL 恒查 0")
    void countByStatus_nullSkipsCondition() {
        when(productMapper.selectCount(any())).thenReturn(7L);

        assertThat(repository.countByStatus(null)).isEqualTo(7L);

        verify(productMapper).selectCount(countWrapperCaptor.capture());
        assertThat(countWrapperCaptor.getValue().getSqlSegment()).doesNotContain("status");
    }

    @Test
    @DisplayName("countByStatus 带状态值时按状态过滤")
    void countByStatus_withStatusFilters() {
        when(productMapper.selectCount(any())).thenReturn(3L);

        assertThat(repository.countByStatus(ProductStatus.PENDING_REVIEW.getCode()))
                .isEqualTo(3L);

        verify(productMapper).selectCount(countWrapperCaptor.capture());
        assertThat(countWrapperCaptor.getValue().getSqlSegment()).contains("status");
    }
}
