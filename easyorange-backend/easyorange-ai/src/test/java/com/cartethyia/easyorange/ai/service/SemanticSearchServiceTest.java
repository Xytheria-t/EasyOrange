package com.cartethyia.easyorange.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.dto.SemanticSearchResult;
import com.cartethyia.easyorange.product.application.port.query.ProductSearchQueryPort;
import com.cartethyia.easyorange.product.application.port.query.SearchResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

@ExtendWith(MockitoExtension.class)
@DisplayName("SemanticSearchService 测试")
class SemanticSearchServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private ProductSearchQueryPort searchQueryPort;

    @Nested
    @DisplayName("search")
    class SearchTests {

        @Test
        @DisplayName("空白关键词返回空结果")
        void search_blankKeyword() {
            SemanticSearchService service = new SemanticSearchService(embeddingModel, Optional.of(searchQueryPort));

            SemanticSearchResult result = service.search("  ", 2, 10);

            assertThat(result.records()).isEmpty();
            assertThat(result.total()).isZero();
            assertThat(result.current()).isEqualTo(2);
            assertThat(result.size()).isEqualTo(10);
            verify(searchQueryPort, never()).search(any());
        }

        @Test
        @DisplayName("null 关键词返回空结果")
        void search_nullKeyword() {
            SemanticSearchService service = new SemanticSearchService(embeddingModel, Optional.of(searchQueryPort));

            SemanticSearchResult result = service.search(null, 1, 10);

            assertThat(result.records()).isEmpty();
            verify(embeddingModel, never()).embed(anyString());
        }

        @Test
        @DisplayName("ES 适配器未配置时返回空结果")
        void search_portEmpty() {
            SemanticSearchService service = new SemanticSearchService(embeddingModel, Optional.empty());

            SemanticSearchResult result = service.search("iPhone", 1, 10);

            assertThat(result.records()).isEmpty();
            verify(embeddingModel, never()).embed(anyString());
        }

        @Test
        @DisplayName("Embedding 为空时返回空结果")
        void search_emptyEmbedding() {
            SemanticSearchService service = new SemanticSearchService(embeddingModel, Optional.of(searchQueryPort));
            when(embeddingModel.embed("iPhone")).thenReturn(new float[0]);

            SemanticSearchResult result = service.search("iPhone", 1, 10);

            assertThat(result.records()).isEmpty();
            verify(searchQueryPort, never()).search(any());
        }

        @Test
        @DisplayName("正常流程 — 查询向量传入 ES 并返回结果")
        void search_success() {
            SemanticSearchService service = new SemanticSearchService(embeddingModel, Optional.of(searchQueryPort));
            when(embeddingModel.embed("编程笔记本")).thenReturn(new float[] {0.1f, 0.2f, 0.3f});
            when(searchQueryPort.search(any(ProductSearchQueryPort.ProductSearchQuery.class)))
                    .thenReturn(new SearchResult(List.of(), 5L, 1, 10, List.of(), List.of(), List.of()));

            SemanticSearchResult result = service.search("编程笔记本", 1, 10);

            assertThat(result.total()).isEqualTo(5);
            assertThat(result.current()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(10);
            verify(searchQueryPort)
                    .search(argThat(q -> q != null
                            && q.queryEmbedding().equals(List.of(0.1f, 0.2f, 0.3f))
                            && q.useSemanticSearch()));
        }
    }
}
