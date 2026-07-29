package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryMapper;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElasticsearchProductSearchIndexAdapterTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductDetailMapper productDetailMapper;

    @Mock
    private ProductImageMapper productImageMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private ElasticsearchProductSearchIndexAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ElasticsearchProductSearchIndexAdapter(
                productMapper, productDetailMapper, productImageMapper,
                categoryMapper, elasticsearchOperations);
    }

    @Test
    @DisplayName("buildDocument 应映射所有字段")
    void buildDocument_shouldMapAllFields() {
        ProductDO product = ProductDO.builder()
                .id("100")
                .userId("200")
                .categoryId("300")
                .name("测试商品")
                .price(new BigDecimal("99.99"))
                .originalPrice(new BigDecimal("199.99"))
                .stock(10)
                .status(ProductStatus.ONLINE)
                .viewCount(1000)
                .conditionLevel(ConditionLevel.GOOD)
                .location("北京")
                .tags("tag1,tag2,tag3")
                .build();

        ProductDetailDO detail = new ProductDetailDO("100", "商品描述");
        when(productDetailMapper.selectById("100")).thenReturn(detail);

        ProductImageDO mainImage = new ProductImageDO("100", "http://example.com/main.jpg", 1, 1);
        ProductImageDO otherImage = new ProductImageDO("100", "http://example.com/other.jpg", 2, 0);
        when(productImageMapper.selectList(any())).thenReturn(List.of(mainImage, otherImage));

        CategoryDO category = new CategoryDO("手机", "0", 1, "icon.png", 1, 1);
        when(categoryMapper.selectById("300")).thenReturn(category);

        ProductDocument doc = adapter.buildDocument(product);

        assertThat(doc.getId()).isEqualTo("100");
        assertThat(doc.getUserId()).isEqualTo("200");
        assertThat(doc.getName()).isEqualTo("测试商品");
        assertThat(doc.getDescription()).isEqualTo("商品描述");
        assertThat(doc.getCategoryId()).isEqualTo("300");
        assertThat(doc.getCategoryName()).isEqualTo("手机");
        assertThat(doc.getPrice()).isEqualTo(99.99);
        assertThat(doc.getOriginalPrice()).isEqualTo(199.99);
        assertThat(doc.getConditionLevel()).isEqualTo("3");
        assertThat(doc.getStatus()).isEqualTo(ProductStatus.ONLINE.getCode());
        assertThat(doc.getViewCount()).isEqualTo(1000);
        assertThat(doc.getStock()).isEqualTo(10);
        assertThat(doc.getLocation()).isEqualTo("北京");
        assertThat(doc.getTags()).containsExactly("tag1", "tag2", "tag3");
        assertThat(doc.getMainImage()).isEqualTo("http://example.com/main.jpg");
        assertThat(doc.getImages()).containsExactly(
                "http://example.com/main.jpg", "http://example.com/other.jpg");
    }

    @Test
    @DisplayName("无图片时应处理为 null/空")
    void buildDocument_shouldHandleNoImages() {
        ProductDO product = ProductDO.builder()
                .id("100")
                .userId("200")
                .categoryId("300")
                .name("测试商品")
                .price(new BigDecimal("99.99"))
                .build();

        when(productDetailMapper.selectById("100")).thenReturn(null);
        when(productImageMapper.selectList(any())).thenReturn(List.of());
        when(categoryMapper.selectById("300")).thenReturn(null);

        ProductDocument doc = adapter.buildDocument(product);

        assertThat(doc.getMainImage()).isNull();
        assertThat(doc.getImages()).isEmpty();
        assertThat(doc.getCategoryName()).isNull();
        assertThat(doc.getDescription()).isNull();
    }

    @Test
    @DisplayName("空标签应返回空列表")
    void buildDocument_shouldHandleBlankTags() {
        ProductDO product = ProductDO.builder()
                .id("100")
                .userId("200")
                .name("测试")
                .price(new BigDecimal("10"))
                .tags("")
                .build();

        when(productDetailMapper.selectById("100")).thenReturn(null);
        when(productImageMapper.selectList(any())).thenReturn(List.of());

        ProductDocument doc = adapter.buildDocument(product);

        assertThat(doc.getTags()).isEmpty();
    }

    @Test
    @DisplayName("主图应取 isMain=1 的图片")
    void buildDocument_shouldPickMainImageByFlag() {
        ProductDO product = ProductDO.builder()
                .id("100")
                .userId("200")
                .name("测试")
                .price(new BigDecimal("10"))
                .build();

        when(productDetailMapper.selectById("100")).thenReturn(null);
        when(productImageMapper.selectList(any())).thenReturn(List.of(
                new ProductImageDO("100", "http://example.com/img1.jpg", 1, 0),
                new ProductImageDO("100", "http://example.com/img2.jpg", 2, 1)
        ));

        ProductDocument doc = adapter.buildDocument(product);

        assertThat(doc.getMainImage()).isEqualTo("http://example.com/img2.jpg");
    }

    @Test
    @DisplayName("无主图标记时应取第一张图片")
    void buildDocument_shouldFallbackToFirstImage() {
        ProductDO product = ProductDO.builder()
                .id("100")
                .userId("200")
                .name("测试")
                .price(new BigDecimal("10"))
                .build();

        when(productDetailMapper.selectById("100")).thenReturn(null);
        when(productImageMapper.selectList(any())).thenReturn(List.of(
                new ProductImageDO("100", "http://example.com/first.jpg", 1, 0)
        ));

        ProductDocument doc = adapter.buildDocument(product);

        assertThat(doc.getMainImage()).isEqualTo("http://example.com/first.jpg");
    }

    @Test
    @DisplayName("indexProduct 应构建并保存文档")
    void indexProduct_shouldBuildDocumentAndSave() {
        ProductDO product = ProductDO.builder()
                .id("100")
                .userId("200")
                .categoryId("300")
                .name("测试商品")
                .price(new BigDecimal("99.99"))
                .build();

        when(productMapper.selectById("100")).thenReturn(product);
        when(productDetailMapper.selectById("100")).thenReturn(null);
        when(productImageMapper.selectList(any())).thenReturn(List.of());
        when(categoryMapper.selectById("300")).thenReturn(null);

        adapter.indexProduct("100");

        verify(elasticsearchOperations).save(any(ProductDocument.class));
    }

    @Test
    @DisplayName("不存在的商品应跳过索引")
    void indexProduct_shouldSkipWhenProductNotFound() {
        when(productMapper.selectById("999")).thenReturn(null);

        adapter.indexProduct("999");

        verify(elasticsearchOperations, never()).save(any(ProductDocument.class));
    }

    @Test
    @DisplayName("removeProductIndex 应删除文档")
    void removeProductIndex_shouldDeleteDocument() {
        adapter.removeProductIndex("100");

        verify(elasticsearchOperations).delete("100", ProductDocument.class);
    }
}
