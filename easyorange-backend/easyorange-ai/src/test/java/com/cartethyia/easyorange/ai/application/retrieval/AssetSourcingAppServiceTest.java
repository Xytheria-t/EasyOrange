package com.cartethyia.easyorange.ai.application.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.port.AssetRetrievalPort;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 找货召回的降级语义分层 —— 同一成功路径两种故障语义：对话侧 {@code search} 三种失败（无适配器 / 检索抛异常）
 * 收敛成「少一次推荐」；MCP 侧 {@code searchStrict} 故障上抛（工具 description 承诺空结果 = 无匹配，
 * 故障伪装成空会让外部模型误判没货而盲目换词重试）。向量化失败两侧同语义：退化 BM25 单路，部分降级 ≠ 不可用。
 */
@DisplayName("AssetSourcingAppService -> 降级语义分层测试")
class AssetSourcingAppServiceTest {

    private static final String QUERY = "5000 以内的笔记本";
    private static final AssetHit HIT =
            new AssetHit("p-1", "MacBook Air M1", new BigDecimal("4800"), "电脑", "九五新", 0.87);

    private EmbeddingModel embeddingModel;
    private ObjectProvider<AssetRetrievalPort> retrievalPortProvider;
    private AssetRetrievalPort port;
    private AiModelSupport aiModelSupport;
    private AssetSourcingAppService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        retrievalPortProvider = mock(ObjectProvider.class);
        port = mock(AssetRetrievalPort.class);
        aiModelSupport = mock(AiModelSupport.class);
        service = new AssetSourcingAppService(embeddingModel, retrievalPortProvider, aiModelSupport);
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

    @Test
    @DisplayName("strict：检索端口抛异常 -> 原样上抛（MCP 协议转错误结果，不伪装成空结果）")
    void searchStrict_portThrows_propagates() {
        when(retrievalPortProvider.getIfAvailable()).thenReturn(port);
        when(aiModelSupport.embed(embeddingModel, AiCallScope.SEMANTIC, QUERY)).thenReturn(List.of(0.1f, 0.2f));
        var failure = new RuntimeException("ES 不可用");
        when(port.search(eq(QUERY), anyList(), eq(5))).thenThrow(failure);

        assertThatThrownBy(() -> service.searchStrict(QUERY, 5)).isSameAs(failure);
    }

    @Test
    @DisplayName("strict：ES 适配器缺失 -> 业务异常（装配问题给统一话术），且不白付 embedding 调用")
    void searchStrict_portMissing_throwsBusinessException() {
        assertThatThrownBy(() -> service.searchStrict(QUERY, 5))
                .isInstanceOf(BaseBusinessException.class)
                .hasMessageContaining("资产检索服务不可用");

        verifyNoInteractions(aiModelSupport);
    }

    @Test
    @DisplayName("strict：向量化失败仍退化 BM25 单路（部分降级 ≠ 不可用，与对话侧同语义）")
    void searchStrict_embedFailed_stillReturnsBm25Results() {
        when(retrievalPortProvider.getIfAvailable()).thenReturn(port);
        when(aiModelSupport.embed(embeddingModel, AiCallScope.SEMANTIC, QUERY))
                .thenThrow(new IllegalStateException("AI 模型未配置：embedding key 为空"));
        when(port.search(eq(QUERY), argThat(List::isEmpty), eq(5))).thenReturn(List.of(HIT));

        assertThat(service.searchStrict(QUERY, 5)).containsExactly(HIT);
    }

    @Test
    @DisplayName("strict：参数非法仍是空列表（client 输入问题不是服务故障）")
    void searchStrict_invalidArguments_returnsEmpty() {
        assertThat(service.searchStrict(null, 5)).isEmpty();
        assertThat(service.searchStrict("   ", 5)).isEmpty();
        assertThat(service.searchStrict(QUERY, 0)).isEmpty();

        verifyNoInteractions(aiModelSupport);
    }
}
