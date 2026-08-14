package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.knowledge.KnowledgeChunk;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeDocEntity;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeIndexPort;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("知识库降级适配器（ES 关闭时）-> 测试")
class KnowledgeFallbackAdapterTest {

    @Mock
    private KnowledgeRepository repository;

    @Test
    @DisplayName("isAvailable=false -> 摄入侧保持 PENDING（启动补索引重试）")
    void unavailable() {
        KnowledgeIndexPort adapter = new KnowledgeFallbackAdapter(repository);

        assertThat(adapter.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("检索降级 -> 按标题/正文 LIKE 返回整篇文档为单块")
    void searchFallsBackToLike() {
        when(repository.searchByContent("退款", 5))
                .thenReturn(List.of(new KnowledgeDocEntity(
                        "kb-0002",
                        "退款规则",
                        "7 天无理由退货…",
                        "平台规则",
                        com.cartethyia.easyorange.ai.enums.KnowledgeDocStatus.INDEXED,
                        3,
                        LocalDateTime.now())));
        KnowledgeIndexPort adapter = new KnowledgeFallbackAdapter(repository);

        List<KnowledgeChunk> chunks = adapter.search("退款", null, 5);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().docId()).isEqualTo("kb-0002");
        assertThat(chunks.getFirst().title()).isEqualTo("退款规则");
        assertThat(chunks.getFirst().embedding()).isNull();
        verify(repository).searchByContent("退款", 5);
    }

    @Test
    @DisplayName("摄入/删除 -> 无索引可写，静默跳过")
    void ingestAndRemoveNoop() {
        KnowledgeIndexPort adapter = new KnowledgeFallbackAdapter(repository);

        adapter.ingestChunks(List.of());
        adapter.removeDoc("kb-0001");

        org.mockito.Mockito.verifyNoInteractions(repository);
    }
}
