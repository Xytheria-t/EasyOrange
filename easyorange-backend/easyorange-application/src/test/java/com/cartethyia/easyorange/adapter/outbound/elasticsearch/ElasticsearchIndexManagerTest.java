package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.Settings;

@ExtendWith(MockitoExtension.class)
class ElasticsearchIndexManagerTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private IndexOperations indexOps;

    private ElasticsearchIndexManager indexManager;

    @BeforeEach
    void setUp() {
        when(elasticsearchOperations.indexOps(ProductDocument.class)).thenReturn(indexOps);
        indexManager = new ElasticsearchIndexManager(elasticsearchOperations);
    }

    @Test
    @DisplayName("索引已存在时应跳过创建")
    void initializeIndex_shouldSkipIfExists() {
        when(indexOps.exists()).thenReturn(true);

        indexManager.createProductIndex();

        verify(indexOps, never()).create(any(Settings.class));
        verify(indexOps, never()).putMapping(any(Document.class));
    }

    @Test
    @DisplayName("索引不存在时应从 JSON 创建")
    void initializeIndex_shouldCreateFromJson() {
        when(indexOps.exists()).thenReturn(false);

        indexManager.createProductIndex();

        verify(indexOps).create(any(Settings.class));
        verify(indexOps).putMapping(any(Document.class));
    }
}
