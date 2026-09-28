package com.cartethyia.easyorange.ai.application.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeMatch;
import com.cartethyia.easyorange.ai.domain.port.KnowledgeIndexPort;
import com.cartethyia.easyorange.ai.testsupport.TestAiModelSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.ObjectProvider;

/** 检索服务（两路召回在索引侧融合后的排序透传与降级）—— 摄入侧在 {@link KnowledgeIngestionAppServiceTest}。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeRetrievalAppService -> 降级口径测试")
class KnowledgeRetrievalAppServiceTest {

    @Mock
    private ObjectProvider<KnowledgeIndexPort> indexPortProvider;

    @Mock
    private ObjectProvider<EmbeddingModel> embeddingModelProvider;

    @Mock
    private KnowledgeIndexPort indexPort;

    @Mock
    private EmbeddingModel embeddingModel;

    private KnowledgeRetrievalAppService retrievalService;

    /** embedding 响应夹具：向量化走 embedForResponse（拿得到响应本体，记账才取得到 usage）。 */
    private static EmbeddingResponse embeddingResponse(float[] vector) {
        return new EmbeddingResponse(List.of(new Embedding(vector, 0)));
    }

    private void setUpRetrieval() {
        retrievalService = new KnowledgeRetrievalAppService(
                indexPortProvider, embeddingModelProvider, TestAiModelSupport.create());
    }

    @Test
    @DisplayName("索引侧已融合排名，服务层按序映射不重排、按 topK 原样透传")
    void search_keepsFusedOrderFromIndex() {
        setUpRetrieval();
        when(indexPortProvider.getIfAvailable()).thenReturn(indexPort);
        when(indexPort.isAvailable()).thenReturn(true);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(embeddingModel.embedForResponse(anyList())).thenReturn(embeddingResponse(new float[] {1f, 0f, 0f}));
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
    @DisplayName("ES 不可用 -> 降级 LIKE 检索（score 恒 0，不请求 embedding）")
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
    @DisplayName("空关键词 -> 空结果")
    void search_blank() {
        setUpRetrieval();

        assertThat(retrievalService.search("  ", 5)).isEmpty();
        assertThat(retrievalService.search("退款", 0)).isEmpty();
    }
}
