package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ReindexServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private ElasticsearchIndexManager indexManager;

    @Mock
    private ElasticsearchProductSearchIndexAdapter indexAdapter;

    @Mock
    private IndexOperations indexOps;

    private ReindexService reindexService;

    @BeforeEach
    void setUp() {
        when(elasticsearchOperations.indexOps(ProductDocument.class)).thenReturn(indexOps);
        reindexService = new ReindexService(productMapper, elasticsearchOperations, indexManager, indexAdapter);
    }

    private Page<ProductDO> pageOf(Collection<ProductDO> records) {
        Page<ProductDO> page = new Page<>(1, 500, false);
        page.setRecords(new ArrayList<>(records));
        page.setTotal(records.size());
        return page;
    }

    @Test
    @DisplayName("全量重建：删除旧索引 → 重建索引 → 分页批量写入（走预加载路径，无 N+1）")
    void reindexAll_shouldRecreateFromMappingAndBulkIndex() {
        when(indexOps.exists()).thenReturn(true);

        ProductDO product1 = ProductDO.builder().id("100").name("商品1").build();
        ProductDO product2 = ProductDO.builder().id("200").name("商品2").build();
        // 首页 2 条（< 500 页大）→ 次页空 → 结束
        when(productMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(pageOf(List.of(product1, product2)))
                .thenReturn(pageOf(List.of()));

        int count = reindexService.reindexAll();

        assertThat(count).isEqualTo(2);
        verify(indexOps).delete();
        verify(indexManager).createProductIndex();
        verify(indexAdapter).indexProducts(List.of("100", "200"));
    }

    @Test
    @DisplayName("无在线商品时重建空索引但跳过批量写入")
    void reindexAll_shouldSkipBulkWriteWhenNoOnlineProducts() {
        when(indexOps.exists()).thenReturn(false);
        when(productMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(pageOf(List.of()));

        int count = reindexService.reindexAll();

        assertThat(count).isZero();
        verify(indexOps, never()).delete();
        verify(indexManager).createProductIndex();
        verify(indexAdapter, never()).indexProducts(any());
    }

    @Test
    @DisplayName("索引不存在时不调用删除，但仍重建索引")
    void reindexAll_shouldNotDeleteWhenIndexMissing() {
        when(indexOps.exists()).thenReturn(false);
        when(productMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(pageOf(List.of()));

        reindexService.reindexAll();

        verify(indexOps, never()).delete();
        verify(indexManager).createProductIndex();
    }
}
