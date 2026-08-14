package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.knowledge.KnowledgeChunk;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeIndexPort;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("知识库 ES 适配器 -> 测试")
class KnowledgeElasticsearchAdapterTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Test
    @DisplayName("写入分块 -> 逐块 save（id = docId:chunkIndex 幂等覆盖）")
    void ingestChunks() {
        KnowledgeIndexPort adapter = new KnowledgeElasticsearchAdapter(elasticsearchOperations, new ObjectMapper());

        adapter.ingestChunks(List.of(
                new KnowledgeChunk("kb-1", 0, "标题", "块0", List.of(0.1f, 0.2f)),
                new KnowledgeChunk("kb-1", 1, "标题", "块1", null)));

        verify(elasticsearchOperations, org.mockito.Mockito.times(2)).save(any(KnowledgeChunkDocument.class));
    }

    @Test
    @DisplayName("删除文档 -> 按 docId term 删除（best-effort 不抛）")
    void removeDoc() {
        KnowledgeIndexPort adapter = new KnowledgeElasticsearchAdapter(elasticsearchOperations, new ObjectMapper());

        adapter.removeDoc("kb-1");

        verify(elasticsearchOperations).delete(any(DeleteQuery.class), any(Class.class));
    }

    @Test
    @DisplayName("写入失败 -> best-effort 不抛异常（索引失败不阻塞主链路）")
    void ingestChunks_bestEffort() {
        when(elasticsearchOperations.save(any(KnowledgeChunkDocument.class)))
                .thenThrow(new RuntimeException("es down"));
        KnowledgeIndexPort adapter = new KnowledgeElasticsearchAdapter(elasticsearchOperations, new ObjectMapper());

        adapter.ingestChunks(List.of(new KnowledgeChunk("kb-1", 0, "标题", "块0", null)));
    }

    @Test
    @DisplayName("ES 启用时 isAvailable=true")
    void available() {
        KnowledgeIndexPort adapter =
                new KnowledgeElasticsearchAdapter(mock(ElasticsearchOperations.class), new ObjectMapper());

        assertThat(adapter.isAvailable()).isTrue();
    }
}
