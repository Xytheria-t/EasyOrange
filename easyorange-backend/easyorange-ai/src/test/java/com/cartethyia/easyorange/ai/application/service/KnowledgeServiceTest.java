package com.cartethyia.easyorange.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.domain.constant.KnowledgeDocStatus;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeChunk;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeDocEntity;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeMatch;
import com.cartethyia.easyorange.ai.domain.port.KnowledgeIndexPort;
import com.cartethyia.easyorange.ai.domain.port.KnowledgeRepository;
import com.cartethyia.easyorange.ai.testsupport.TestAiModelSupport;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("知识库摄入/检索 -> 测试")
class KnowledgeServiceTest {

    @Mock
    private KnowledgeRepository repository;

    @Mock
    private ObjectProvider<KnowledgeIndexPort> indexPortProvider;

    @Mock
    private ObjectProvider<EmbeddingModel> embeddingModelProvider;

    @Mock
    private KnowledgeIndexPort indexPort;

    @Mock
    private EmbeddingModel embeddingModel;

    private KnowledgeIngestionService ingestionService;
    private KnowledgeRetrievalService retrievalService;

    private void setUpIngestion() {
        ingestionService = new KnowledgeIngestionService(
                repository, indexPortProvider, embeddingModelProvider, TestAiModelSupport.create());
    }

    private void setUpRetrieval() {
        retrievalService =
                new KnowledgeRetrievalService(indexPortProvider, embeddingModelProvider, TestAiModelSupport.create());
    }

    // ---------- 分块算法 ----------

    @Test
    @DisplayName("分块：500 字一块 + 50 字重叠，长文切 3 块")
    void chunk_contentSplitsWithOverlap() {
        String content = "块".repeat(1200);

        List<String> chunks = KnowledgeIngestionService.chunkContent(content);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(500);
        assertThat(chunks.get(1)).hasSize(500);
        assertThat(chunks.get(2).length()).isBetween(200, 500);
        // 重叠：第 2 块起点 = 500 - 50
        assertThat(chunks.get(1)).startsWith(content.substring(450, 460));
    }

    @Test
    @DisplayName("分块：切点优先落在换行处（不切断句子）")
    void chunk_prefersNewlineBoundary() {
        String content = "句".repeat(400) + "\n" + "句".repeat(200);

        List<String> chunks = KnowledgeIngestionService.chunkContent(content);

        assertThat(chunks).hasSize(2);
        // 切点落在 400 处（换行位置），第一块不含换行后的内容
        assertThat(chunks.get(0)).hasSize(400).doesNotContain("\n");
        assertThat(chunks.get(1)).contains("\n");
    }

    @Test
    @DisplayName("分块：空文本 -> 空列表")
    void chunk_blank() {
        assertThat(KnowledgeIngestionService.chunkContent(null)).isEmpty();
        assertThat(KnowledgeIngestionService.chunkContent("  ")).isEmpty();
    }

    // ---------- 摄入管线 ----------

    @Test
    @DisplayName("摄入：落库 -> 分块 embed -> 写 ES -> 回填 INDEXED")
    void ingest_happyPath() {
        setUpIngestion();
        when(indexPortProvider.getIfAvailable()).thenReturn(indexPort);
        when(indexPort.isAvailable()).thenReturn(true);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(repository.save(any())).thenReturn("doc-1");
        when(embeddingModel.embed(anyString())).thenReturn(new float[] {1f, 0f, 0f});

        ingestionService.ingest("交易流程", "步骤".repeat(400), "平台规则");

        ArgumentCaptor<List<KnowledgeChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(indexPort).ingestChunks(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().getFirst().docId()).isEqualTo("doc-1");
        verify(repository).updateStatus("doc-1", KnowledgeDocStatus.INDEXED, 2);
    }

    @Test
    @DisplayName("摄入：索引不可用 -> 保持 PENDING 不写索引")
    void ingest_indexUnavailable() {
        setUpIngestion();
        when(indexPortProvider.getIfAvailable()).thenReturn(indexPort);
        when(indexPort.isAvailable()).thenReturn(false);
        when(repository.save(any())).thenReturn("doc-1");

        ingestionService.ingest("标题", "内容", "来源");

        verify(indexPort, never()).ingestChunks(any());
        verify(repository, never()).updateStatus(anyString(), any(), any(Integer.class));
    }

    @Test
    @DisplayName("摄入：embed 失败 -> 块照常写入（best-effort）")
    void ingest_embedFailsStillIndexes() {
        setUpIngestion();
        when(indexPortProvider.getIfAvailable()).thenReturn(indexPort);
        when(indexPort.isAvailable()).thenReturn(true);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(repository.save(any())).thenReturn("doc-1");
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("embed api down"));

        ingestionService.ingest("标题", "内容内容内容内容内容内容内容内容内容内容", "来源");

        ArgumentCaptor<List<KnowledgeChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(indexPort).ingestChunks(captor.capture());
        assertThat(captor.getValue().getFirst().embedding()).isNull();
        verify(repository).updateStatus("doc-1", KnowledgeDocStatus.INDEXED, 1);
    }

    @Test
    @DisplayName("补索引：只重试 PENDING 文档且保持原 ID")
    void reindexPending_keepsId() {
        setUpIngestion();
        when(repository.findById("doc-1"))
                .thenReturn(Optional.of(
                        new KnowledgeDocEntity("doc-1", "标题", "内容", "来源", KnowledgeDocStatus.PENDING, 0, null)));
        when(repository.findById("doc-2"))
                .thenReturn(Optional.of(
                        new KnowledgeDocEntity("doc-2", "标题2", "内容2", "来源", KnowledgeDocStatus.INDEXED, 1, null)));
        when(indexPortProvider.getIfAvailable()).thenReturn(indexPort);
        when(indexPort.isAvailable()).thenReturn(true);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(embeddingModel.embed(anyString())).thenReturn(new float[] {1f, 0f, 0f});

        ingestionService.reindexPending("doc-1");
        ingestionService.reindexPending("doc-2");

        ArgumentCaptor<List<KnowledgeChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(indexPort).ingestChunks(captor.capture());
        assertThat(captor.getValue().getFirst().docId()).isEqualTo("doc-1");
        verify(repository).updateStatus("doc-1", KnowledgeDocStatus.INDEXED, 1);
    }

    // ---------- 检索 ----------

    @Test
    @DisplayName("检索：索引侧已融合排名，服务层按序映射不重排、按 topK 原样透传")
    void search_keepsFusedOrderFromIndex() {
        setUpRetrieval();
        when(indexPortProvider.getIfAvailable()).thenReturn(indexPort);
        when(indexPort.isAvailable()).thenReturn(true);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(embeddingModel.embed(anyString())).thenReturn(new float[] {1f, 0f, 0f});
        // 融合分由索引侧算好：kb-a 排前是因为两路都命中，光看余弦 kb-b 反而更近
        when(indexPort.search("退款", List.of(1f, 0f, 0f), 2))
                .thenReturn(List.of(
                        new KnowledgeMatch("kb-a", 0, "A", "退款规则", 1.0 / 61 + 1.0 / 61),
                        new KnowledgeMatch("kb-b", 0, "B", "不相关内容", 1.0 / 62)));

        List<KnowledgeHit> hits = retrievalService.search("退款", 2);

        assertThat(hits).hasSize(2);
        assertThat(hits.getFirst().docId()).isEqualTo("kb-a");
        assertThat(hits.get(1).docId()).isEqualTo("kb-b");
        // topK 不再被放大成 2 倍：候选池大小由索引侧决定，服务层不再做「多取再截断」
        verify(indexPort).search("退款", List.of(1f, 0f, 0f), 2);
    }

    @Test
    @DisplayName("检索：ES 不可用 -> 降级 LIKE 检索（score 恒 0，不请求 embedding）")
    void search_fallback() {
        setUpRetrieval();
        when(indexPortProvider.getIfAvailable()).thenReturn(indexPort);
        when(indexPort.isAvailable()).thenReturn(false);
        when(indexPort.search("退款", null, 2)).thenReturn(List.of(new KnowledgeMatch("kb-0002", 0, "退款规则", "内容", 0)));

        List<KnowledgeHit> hits = retrievalService.search("退款", 2);

        assertThat(hits).hasSize(1);
        assertThat(hits.getFirst().docId()).isEqualTo("kb-0002");
        assertThat(hits.getFirst().score()).isEqualTo(0);
        verify(embeddingModelProvider, never()).getIfAvailable();
    }

    @Test
    @DisplayName("检索：空关键词 -> 空结果")
    void search_blank() {
        setUpRetrieval();

        assertThat(retrievalService.search("  ", 5)).isEmpty();
        assertThat(retrievalService.search("退款", 0)).isEmpty();
    }
}
