package com.cartethyia.easyorange.adapter.inbound.job;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.config.SearchEnhanceWarmupProperties;
import com.cartethyia.easyorange.product.application.port.query.AiSearchEnhancerPort;
import com.cartethyia.easyorange.product.application.query.ProductSearchCriteria;
import com.cartethyia.easyorange.product.application.query.ProductSearchQueryHandler;
import com.cartethyia.easyorange.product.application.query.dto.ProductSearchResult;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;

@DisplayName("AiSearchEnhancementWarmup 测试")
class AiSearchEnhancementWarmupTest {

    private static final String KEYWORD = "适合拍夜景的相机";

    private final ProductSearchQueryHandler searchQueryHandler = mock(ProductSearchQueryHandler.class);
    private final AiSearchEnhancerPort enhancer = mock(AiSearchEnhancerPort.class);

    @Test
    @DisplayName("启动触发一次：配置查询串走完整链路，热词只跑增强，均强制刷新")
    void startupTriggersOncePerKeyword() {
        when(searchQueryHandler.search(any(), anyBoolean(), eq(true))).thenReturn(searchResult());
        when(searchQueryHandler.getHotKeywords(5)).thenReturn(List.of(new HotKeywordReadModel("1", "相机", 3, 1)));

        warmup(true, List.of(KEYWORD), "test-key").run(null);

        // forceRefresh=true：命中式预热在 TTL > 刷新间隔时会留出冷查空窗
        verify(searchQueryHandler).search(argThat(keywordIs(KEYWORD)), eq(true), eq(true));
        verify(searchQueryHandler).search(argThat(keywordIs("相机")), eq(false), eq(true));
    }

    @Test
    @DisplayName("检索抛异常不影响启动：异常收敛在单条关键词内")
    void failureDoesNotEscape() {
        when(searchQueryHandler.search(any(), anyBoolean(), eq(true))).thenThrow(new RuntimeException("es down"));
        when(searchQueryHandler.getHotKeywords(5)).thenReturn(List.of());

        AiSearchEnhancementWarmup warmup = warmup(true, List.of(KEYWORD), "test-key");

        assertThatCode(() -> warmup.run(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("未配置文本模型 key：整段跳过，不打模型")
    void skipsWithoutApiKey() {
        AiSearchEnhancementWarmup warmup = warmup(true, List.of(KEYWORD), "  ");

        warmup.run(null);
        warmup.scheduledRefresh();

        verify(searchQueryHandler, never()).search(any(), anyBoolean(), anyBoolean());
        verify(searchQueryHandler, never()).getHotKeywords(anyInt());
    }

    @Test
    @DisplayName("开关关闭：不检索也不刷新")
    void skipsWhenDisabled() {
        AiSearchEnhancementWarmup warmup = warmup(false, List.of(KEYWORD), "test-key");

        warmup.run(null);

        verify(searchQueryHandler, never()).search(any(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("同 key 预热未完成时并发的定时刷新不重复打模型")
    void concurrentTriggerIsDeduped() {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(searchQueryHandler.search(argThat(keywordIs(KEYWORD)), eq(true), eq(true)))
                .thenAnswer(invocation -> {
                    entered.countDown();
                    assertThatCode(() -> release.await(5, TimeUnit.SECONDS)).doesNotThrowAnyException();
                    return searchResult();
                });
        when(searchQueryHandler.getHotKeywords(5)).thenReturn(List.of());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            AiSearchEnhancementWarmup warmup = warmupOn(executor::execute, true, List.of(KEYWORD), "test-key");
            executor.submit(() -> warmup.run(null));
            assertThatCode(() -> entered.await(5, TimeUnit.SECONDS)).doesNotThrowAnyException();

            warmup.scheduledRefresh();
            release.countDown();
        } finally {
            executor.shutdownNow();
        }

        verify(searchQueryHandler, times(1)).search(argThat(keywordIs(KEYWORD)), eq(true), eq(true));
    }

    private AiSearchEnhancementWarmup warmup(boolean enabled, List<String> queries, String apiKey) {
        return warmupOn(Runnable::run, enabled, queries, apiKey);
    }

    private AiSearchEnhancementWarmup warmupOn(
            TaskExecutor executor, boolean enabled, List<String> queries, String apiKey) {
        @SuppressWarnings("unchecked")
        ObjectProvider<AiSearchEnhancerPort> enhancerProvider = mock(ObjectProvider.class);
        when(enhancerProvider.getIfAvailable()).thenReturn(enhancer);
        @SuppressWarnings("unchecked")
        ObjectProvider<TaskExecutor> executorProvider = mock(ObjectProvider.class);
        when(executorProvider.getIfAvailable()).thenReturn(executor);
        return new AiSearchEnhancementWarmup(
                searchQueryHandler,
                enhancerProvider,
                executorProvider,
                new SearchEnhanceWarmupProperties(enabled, queries, 240_000L),
                apiKey);
    }

    private static org.mockito.ArgumentMatcher<ProductSearchCriteria> keywordIs(String keyword) {
        return criteria -> keyword.equals(criteria.keyword());
    }

    private static ProductSearchResult searchResult() {
        List<ProductReadModel> records =
                List.of(ProductReadModel.builder().id("p1").title(KEYWORD).build());
        AiEnhancement enhancement =
                new AiEnhancement("夜景拍摄意图", Map.of("p1", List.of("📸实拍")), "价格区间正常", List.of("预算？"));
        return new ProductSearchResult(PageResult.of(records, 1, 1, 5), List.of(), enhancement, false);
    }
}
