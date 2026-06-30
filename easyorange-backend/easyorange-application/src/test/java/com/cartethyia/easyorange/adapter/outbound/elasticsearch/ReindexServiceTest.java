package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReindexServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private ElasticsearchProductSearchIndexAdapter indexAdapter;

    @Mock
    private IndexOperations indexOps;

    private ReindexService reindexService;

    @BeforeEach
    void setUp() {
        when(elasticsearchOperations.indexOps(ProductDocument.class)).thenReturn(indexOps);
        reindexService = new ReindexService(productMapper, elasticsearchOperations, indexAdapter);
    }

    @Test
    @DisplayName("全量重建应删除旧索引并写入新数据")
    void reindexAll_shouldRecreateIndexAndIndexProducts() {
        when(indexOps.exists()).thenReturn(true);

        ProductDO product1 = ProductDO.builder().id("100").name("商品1").build();
        ProductDO product2 = ProductDO.builder().id("200").name("商品2").build();
        List<ProductDO> products = List.of(product1, product2);
        when(productMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(products);

        ProductDocument doc1 = ProductDocument.builder().id("100").name("商品1").build();
        ProductDocument doc2 = ProductDocument.builder().id("200").name("商品2").build();
        when(indexAdapter.buildDocument(product1)).thenReturn(doc1);
        when(indexAdapter.buildDocument(product2)).thenReturn(doc2);

        int count = reindexService.reindexAll();

        assertThat(count).isEqualTo(2);
        verify(indexOps).delete();
        verify(elasticsearchOperations).save(anyList());
    }

    @Test
    @DisplayName("无在线商品时应跳过批量保存")
    void reindexAll_shouldHandleEmptyProducts() {
        when(indexOps.exists()).thenReturn(false);
        when(productMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        int count = reindexService.reindexAll();

        assertThat(count).isZero();
        verify(indexOps, never()).delete();
        verify(elasticsearchOperations, never()).save(anyList());
    }

    @Test
    @DisplayName("索引不存在时不调用删除")
    void reindexAll_shouldNotDeleteIfIndexNotExists() {
        when(indexOps.exists()).thenReturn(false);
        when(productMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        reindexService.reindexAll();

        verify(indexOps, never()).delete();
    }
}