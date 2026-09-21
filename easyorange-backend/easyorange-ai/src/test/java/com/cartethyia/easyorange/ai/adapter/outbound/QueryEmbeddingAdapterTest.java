package com.cartethyia.easyorange.ai.adapter.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * 重点在「永不抛异常」这条契约：本类挂在商品检索主链路上，调用方不做兜底，
 * 它一抛就是把整个搜索接口打成 500。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QueryEmbeddingAdapter 测试")
class QueryEmbeddingAdapterTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private AiModelSupport aiModelSupport;

    private QueryEmbeddingAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new QueryEmbeddingAdapter(embeddingModel, aiModelSupport);
    }

    @Test
    @DisplayName("空白 / null 关键词不调模型，直接返回空列表")
    void embed_blankText_shouldSkipModel() {
        assertThat(adapter.embed("   ")).isEmpty();
        assertThat(adapter.embed(null)).isEmpty();

        verifyNoInteractions(aiModelSupport);
    }

    @Test
    @DisplayName("模型未配置（调用抛异常）时返回空列表而不是抛出：检索要能退化为纯 BM25")
    void embed_modelFailure_shouldReturnEmptyInsteadOfThrowing() {
        when(aiModelSupport.embed(any(), any(), anyString()))
                .thenThrow(new IllegalStateException("AI 模型未配置：easyorange.ai.embedding.api-key 为空"));

        assertThat(adapter.embed("手机")).isEmpty();
    }

    @Test
    @DisplayName("正常路径按 SEMANTIC 场景记账并返回向量")
    void embed_success_shouldReturnVector() {
        when(aiModelSupport.embed(any(), eq(AiCallScope.SEMANTIC), eq("手机"))).thenReturn(List.of(0.1f, 0.2f));

        assertThat(adapter.embed("手机")).containsExactly(0.1f, 0.2f);
    }
}
