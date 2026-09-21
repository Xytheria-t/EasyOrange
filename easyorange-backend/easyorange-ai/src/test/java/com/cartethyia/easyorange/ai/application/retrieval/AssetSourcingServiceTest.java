package com.cartethyia.easyorange.ai.application.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.port.AssetRetrievalPort;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 找货召回的降级口径 —— 三种失败（无适配器 / 向量化失败 / 检索抛异常）都收敛成「少一次推荐」，
 * 不把异常抛给对话循环与 MCP 工具面。
 */
@DisplayName("AssetSourcingService -> 降级口径测试")
class AssetSourcingServiceTest {

    private static final String QUERY = "5000 以内的笔记本";
    private static final AssetHit HIT =
            new AssetHit("p-1", "MacBook Air M1", new BigDecimal("4800"), "电脑", "九五新", 0.87);

    private EmbeddingModel embeddingModel;
    private ObjectProvider<AssetRetrievalPort> retrievalPortProvider;
    private AssetRetrievalPort port;
    private AiModelSupport aiModelSupport;
    private AssetSourcingService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        retrievalPortProvider = mock(ObjectProvider.class);
        port = mock(AssetRetrievalPort.class);
        aiModelSupport = mock(AiModelSupport.class);
        service = new AssetSourcingService(embeddingModel, retrievalPortProvider, aiModelSupport);
    }

    @Test
    @DisplayName("向量化抛异常（key 未配置的占位模型 / 供应商故障）-> 退化为 BM25 单路召回，不抛给调用方")
    void search_embedFailed_fallsBackToBm25Only() {
        when(retrievalPortProvider.getIfAvailable()).thenReturn(port);
        when(aiModelSupport.embed(embeddingModel, AiCallScope.SEMANTIC, QUERY))
                .thenThrow(new IllegalStateException("AI 模型未配置：embedding key 为空"));
        when(port.search(eq(QUERY), argThat(List::isEmpty), eq(5))).thenReturn(List.of(HIT));

        assertThat(service.search(QUERY, 5)).containsExactly(HIT);
    }

    @Test
    @DisplayName("检索端口抛异常 -> 返回空列表（对话侧少一次推荐，不整轮失败）")
    void search_portThrows_returnsEmpty() {
        when(retrievalPortProvider.getIfAvailable()).thenReturn(port);
        when(aiModelSupport.embed(embeddingModel, AiCallScope.SEMANTIC, QUERY)).thenReturn(List.of(0.1f, 0.2f));
        when(port.search(eq(QUERY), anyList(), eq(5))).thenThrow(new RuntimeException("ES 不可用"));

        assertThat(service.search(QUERY, 5)).isEmpty();
    }

    @Test
    @DisplayName("ES 适配器缺失 -> 空列表，且不做向量化（不白付一次 embedding 调用）")
    void search_portMissing_skipsEmbedding() {
        assertThat(service.search(QUERY, 5)).isEmpty();

        verifyNoInteractions(aiModelSupport);
    }

    @Test
    @DisplayName("空查询 / 非正 topK -> 空列表，不进检索链路")
    void search_invalidArguments_returnsEmpty() {
        assertThat(service.search(null, 5)).isEmpty();
        assertThat(service.search("   ", 5)).isEmpty();
        assertThat(service.search(QUERY, 0)).isEmpty();

        verifyNoInteractions(aiModelSupport);
    }
}
