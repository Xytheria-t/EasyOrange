package com.cartethyia.easyorange.ai.application.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.domain.enums.KnowledgeDocStatus;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeChunk;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeDocEntity;
import com.cartethyia.easyorange.ai.domain.port.KnowledgeIndexPort;
import com.cartethyia.easyorange.ai.domain.port.KnowledgeRepository;
import com.cartethyia.easyorange.ai.testsupport.TestAiModelSupport;
import com.cartethyia.easyorange.common.result.PageResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.ObjectProvider;

/** 摄入管线（分块 / 落库 / 索引 / 补索引）—— 检索侧在 {@link KnowledgeRetrievalAppServiceTest}。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeIngestionAppService -> 测试")
class KnowledgeIngestionAppServiceTest {

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

    private KnowledgeIngestionAppService ingestionService;

    /** embedding 响应夹具：向量化走 embedForResponse（拿得到响应本体，记账才取得到 usage），批量按请求条数逐条返回。 */
    private static EmbeddingResponse embeddingResponse(float[] vector, int count) {
        var embeddings = new ArrayList<Embedding>(count);
        for (int i = 0; i < count; i++) {
            embeddings.add(new Embedding(vector, i));
        }
        return new EmbeddingResponse(embeddings);
    }

    private void setUpIngestion() {
        ingestionService = new KnowledgeIngestionAppService(
                repository, indexPortProvider, embeddingModelProvider, TestAiModelSupport.create());
    }

    // ---------- 分块算法 ----------

    @Test
    @DisplayName("分块：500 字一块 + 50 字重叠，长文切 3 块")
    void chunk_contentSplitsWithOverlap() {
        String content = "块".repeat(1200);

        List<String> chunks = KnowledgeIngestionAppService.chunkContent(content);

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

        List<String> chunks = KnowledgeIngestionAppService.chunkContent(content);

        assertThat(chunks).hasSize(2);
        // 切点落在 400 处（换行位置），第一块不含换行后的内容
        assertThat(chunks.get(0)).hasSize(400).doesNotContain("\n");
        assertThat(chunks.get(1)).contains("\n");
    }

    @Test
    @DisplayName("分块：空文本 -> 空列表")
    void chunk_blank() {
        assertThat(KnowledgeIngestionAppService.chunkContent(null)).isEmpty();
        assertThat(KnowledgeIngestionAppService.chunkContent("  ")).isEmpty();
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
        when(embeddingModel.embedForResponse(anyList()))
                .thenAnswer(
                        inv -> embeddingResponse(new float[] {1f, 0f, 0f}, ((List<String>) inv.getArgument(0)).size()));

        ingestionService.ingest("交易流程", "步骤".repeat(400), "平台规则");

        ArgumentCaptor<List<KnowledgeChunk>> captor = ArgumentCaptor.forClass(List.class);
        // 重摄幂等：写入前先按 docId 清旧块（内容改版块数变少时不留孤儿）
        verify(indexPort).removeDoc("doc-1");
        verify(indexPort).ingestChunks(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().getFirst().docId()).isEqualTo("doc-1");
        assertThat(captor.getValue().get(1).embedding()).isNotNull();
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
    @DisplayName("摄入：embed 失败 -> 整篇标 FAILED 待补索引（不再「缺向量块照写」掩盖静默缺失）")
    void ingest_embedFailsMarksFailed() {
        setUpIngestion();
        when(indexPortProvider.getIfAvailable()).thenReturn(indexPort);
        when(indexPort.isAvailable()).thenReturn(true);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(repository.save(any())).thenReturn("doc-1");
        when(embeddingModel.embedForResponse(anyList())).thenThrow(new RuntimeException("embed api down"));

        ingestionService.ingest("标题", "内容内容内容内容内容内容内容内容内容内容", "来源");

        verify(indexPort, never()).ingestChunks(any());
        verify(repository).updateStatus("doc-1", KnowledgeDocStatus.FAILED, 1);
    }

    @Test
    @DisplayName("摄入：ES 写入失败 -> 整篇标 FAILED（适配器不再吞异常假标 INDEXED）")
    void ingest_indexWriteFailsMarksFailed() {
        setUpIngestion();
        when(indexPortProvider.getIfAvailable()).thenReturn(indexPort);
        when(indexPort.isAvailable()).thenReturn(true);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(repository.save(any())).thenReturn("doc-1");
        when(embeddingModel.embedForResponse(anyList()))
                .thenAnswer(
                        inv -> embeddingResponse(new float[] {1f, 0f, 0f}, ((List<String>) inv.getArgument(0)).size()));
        doThrow(new RuntimeException("es write failed")).when(indexPort).ingestChunks(any());

        ingestionService.ingest("标题", "内容", "来源");

        verify(repository).updateStatus("doc-1", KnowledgeDocStatus.FAILED, 1);
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
        when(embeddingModel.embedForResponse(anyList()))
                .thenAnswer(
                        inv -> embeddingResponse(new float[] {1f, 0f, 0f}, ((List<String>) inv.getArgument(0)).size()));

        ingestionService.reindexPending("doc-1");
        ingestionService.reindexPending("doc-2");

        ArgumentCaptor<List<KnowledgeChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(indexPort).ingestChunks(captor.capture());
        assertThat(captor.getValue().getFirst().docId()).isEqualTo("doc-1");
        verify(repository).updateStatus("doc-1", KnowledgeDocStatus.INDEXED, 1);
    }

    @Test
    @DisplayName("补索引：FAILED 文档一并重试（embed/写失败的未完成态，与 PENDING 同口径）")
    void reindexPending_retriesFailedDocs() {
        setUpIngestion();
        when(repository.findById("doc-1"))
                .thenReturn(Optional.of(
                        new KnowledgeDocEntity("doc-1", "标题", "内容", "来源", KnowledgeDocStatus.FAILED, 1, null)));
        when(indexPortProvider.getIfAvailable()).thenReturn(indexPort);
        when(indexPort.isAvailable()).thenReturn(true);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(embeddingModel.embedForResponse(anyList()))
                .thenAnswer(
                        inv -> embeddingResponse(new float[] {1f, 0f, 0f}, ((List<String>) inv.getArgument(0)).size()));

        ingestionService.reindexPending("doc-1");

        verify(repository).updateStatus("doc-1", KnowledgeDocStatus.INDEXED, 1);
    }

    @Test
    @DisplayName("补索引：INDEXED 文档跳过（已完成态不重摄）")
    void reindexIncomplete_skipsIndexedDocs() {
        setUpIngestion();
        when(repository.page(1, 50))
                .thenReturn(new PageResult<>(
                        List.of(new KnowledgeDocEntity("doc-1", "标题", "内容", "来源", KnowledgeDocStatus.INDEXED, 1, null)),
                        1,
                        1,
                        1,
                        1));

        int retried = ingestionService.reindexIncomplete();

        assertThat(retried).isZero();
        verify(repository, never()).updateStatus(anyString(), any(), any(Integer.class));
    }
}
