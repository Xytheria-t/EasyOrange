package com.cartethyia.easyorange.ai.adapter.outbound;

import com.cartethyia.easyorange.ai.adapter.outbound.tool.IntentDetectionTool;
import com.cartethyia.easyorange.ai.adapter.outbound.tool.MarketAnalysisTool;
import com.cartethyia.easyorange.ai.adapter.outbound.tool.ProductTaggingTool;
import com.cartethyia.easyorange.ai.adapter.outbound.tool.QuestionSuggestionTool;
import com.cartethyia.easyorange.ai.adapter.outbound.tool.SearchToolContext;
import com.cartethyia.easyorange.ai.adapter.outbound.tool.SearchToolRegistry;
import com.cartethyia.easyorange.ai.application.enhancement.NaturalLanguageDetector;
import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.product.application.port.query.AiSearchEnhancerPort;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

/**
 * AI 导购搜索增强管道 — 4 路并行 Tool Calling（Tool Registry 模式）。
 * <p>
 * 用户自然语言查询进来，从 {@link SearchToolRegistry} 取 4 个工具并行执行：
 * intent_detection（LLM 意图识别）/ product_tagging（规则标签）/ market_analysis（规则价格统计）/
 * question_suggestion（规则追问模板）。<b>4 路里只有 1 路打模型</b> —— 另外三路的产出本来就能在本地算出来，
 * 交给模型既多付一次调用，又会在供应商变慢时超时缺席；而「降级不写缓存」意味着缺席会被每次搜索重算一遍。
 * 单步骤失败降级不影响整体，四路并行的总等待有上限
 * （{@code easyorange.ai.search-enhance.timeout-seconds}，默认 5s；供应商越慢越要调大），结果经 Redis 5min TTL 缓存。
 * <p>
 * <b>对上游的契约是「永不抛异常」</b>：本类挂在商品检索主链路上（{@code ProductSearchQueryHandler}
 * 不做异常兜底），任何意外失败都返回 {@link Optional#empty()}，让检索退化为「无 AI 增强」而不是整个接口 500。
 * <p>
 * <b>降级结果不进缓存</b>：超时/异常分支只把残缺结果返回给本次请求，不写 Redis ——
 * 供应商抖动一次若被缓存 5 分钟，会让「降级」在缓存 TTL 内固化成「正常结果」。
 * 代价是抖动期间每次请求都重试 LLM，这是刻意选择：宁可多花几次调用，也不伪装成正常。
 */
@Slf4j
@Primary
@Component
public class AiSearchEnhancerAdapter implements AiSearchEnhancerPort {

    private final NaturalLanguageDetector nlDetector;
    private final SearchToolRegistry toolRegistry;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final int timeoutSeconds;

    private static final long CACHE_TTL_MINUTES = 5;
    private static final String CACHE_KEY_PREFIX = "ai:search:enhance:";
    private static final int TOP_PRODUCTS_LIMIT = 5;

    public AiSearchEnhancerAdapter(
            NaturalLanguageDetector nlDetector,
            SearchToolRegistry toolRegistry,
            ObjectProvider<RedisTemplate<Object, Object>> redisTemplateProvider,
            @Value("${easyorange.ai.search-enhance.timeout-seconds:5}") int timeoutSeconds) {
        this.nlDetector = nlDetector;
        this.toolRegistry = toolRegistry;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public Optional<AiEnhancement> tryEnhance(String keyword, List<ProductReadModel> topProducts) {
        try {
            return doEnhance(keyword, topProducts);
        } catch (Exception e) {
            // 契约：异常不越过 Port 边界（调用方无兜底，逃逸即整个检索接口失败）
            log.warn("AI search enhancement failed, search proceeds without enhancement, keyword={}", keyword, e);
            return Optional.empty();
        }
    }

    private Optional<AiEnhancement> doEnhance(String keyword, List<ProductReadModel> topProducts) {
        if (!nlDetector.isNaturalLanguage(keyword)) {
            return Optional.empty();
        }
        if (topProducts == null || topProducts.isEmpty()) {
            return Optional.empty();
        }

        String cacheKey = CACHE_KEY_PREFIX + md5(keyword);

        if (redisTemplate != null) {
            try {
                // 反序列化失败/类型不符时抛异常，由下方 catch 降级重算（原 CacheUtils.cast 语义等价）
                AiEnhancement cached =
                        (AiEnhancement) redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("AI enhancement cache hit for keyword: {}", keyword);
                    return Optional.of(cached);
                }
            } catch (Exception e) {
                log.debug("Cache read failed for key {}: {}", cacheKey, e.getMessage());
            }
        }

        List<ProductReadModel> top5 = topProducts.subList(0, Math.min(TOP_PRODUCTS_LIMIT, topProducts.size()));
        var context = new SearchToolContext(keyword, top5);

        // 工具名取各工具自己的常量，不在编排器里再写一遍字面量：
        // 名字只在工具类里定义一次，改名不会有「注册表里查不到 → 静默降级」的窗口
        CompletableFuture<String> intentFuture = runTool(IntentDetectionTool.NAME, context);
        CompletableFuture<Map<String, List<String>>> tagsFuture = runTool(ProductTaggingTool.NAME, context);
        CompletableFuture<String> marketFuture = runTool(MarketAnalysisTool.NAME, context);
        CompletableFuture<List<String>> questionsFuture = runTool(QuestionSuggestionTool.NAME, context);

        try {
            CompletableFuture.allOf(intentFuture, tagsFuture, marketFuture, questionsFuture)
                    .get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("AI search enhancement timed out for keyword: {}", keyword);
            // cancel(false) 不中断已开始的 LLM 调用（CompletableFuture 不支持中断），在飞调用会跑到
            // 客户端超时才结束；这里只是避免未开始的任务继续调度，并回收本次请求要用的部分结果。
            intentFuture.cancel(false);
            marketFuture.cancel(false);
            questionsFuture.cancel(false);
            return collectPartialResults(intentFuture, tagsFuture, marketFuture, questionsFuture);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("AI search enhancement interrupted for keyword: {}", keyword);
            return Optional.empty();
        } catch (ExecutionException e) {
            log.warn("AI search enhancement failed for keyword: {}", keyword, e.getCause());
            return collectPartialResults(intentFuture, tagsFuture, marketFuture, questionsFuture);
        }

        String intentExplanation = intentFuture.getNow(null);
        Map<String, List<String>> productTags = tagsFuture.getNow(Map.of());

        String marketAnalysis = marketFuture.getNow(null);
        List<String> suggestedQuestions = questionsFuture.getNow(List.of());

        if (intentExplanation == null && productTags.isEmpty()) {
            return Optional.empty();
        }

        // 只有四路全成功的路径才写缓存（降级结果已在上面直接返回）
        AiEnhancement result = new AiEnhancement(intentExplanation, productTags, marketAnalysis, suggestedQuestions);
        writeToCache(cacheKey, result);
        return Optional.of(result);
    }

    /**
     * 从注册表取工具并提交并行执行。
     * <p>
     * 按 {@link com.cartethyia.easyorange.ai.adapter.outbound.tool.SearchTool} 的失败约定，
     * 工具**不吞异常**：LLM 故障会让对应 future 异常完成，由 {@code allOf(...).get()}
     * 抛 {@link ExecutionException}，本类据此判定「本次降级」并放弃写缓存
     * （若工具把异常吞成空值，管道就分不清「正常空结果」与「本次降级」，抖动会被缓存固化 5 分钟）。
     */
    @SuppressWarnings("unchecked")
    private <T> CompletableFuture<T> runTool(String name, SearchToolContext context) {
        return (CompletableFuture<T>) toolRegistry.get(name).run(context);
    }

    private void writeToCache(String cacheKey, AiEnhancement enhancement) {
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, enhancement, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.debug("AI enhancement cached for key: {}", cacheKey);
            } catch (Exception e) {
                log.debug("Cache write failed for key {}: {}", cacheKey, e.getMessage());
            }
        }
    }

    private static String md5(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }

    private Optional<AiEnhancement> collectPartialResults(
            CompletableFuture<String> intentFuture,
            CompletableFuture<Map<String, List<String>>> tagsFuture,
            CompletableFuture<String> marketFuture,
            CompletableFuture<List<String>> questionsFuture) {

        String intentExplanation = getOrNull(intentFuture);
        Map<String, List<String>> productTags = getOrDefault(tagsFuture, Map.of());
        String marketAnalysis = getOrNull(marketFuture);
        List<String> suggestedQuestions = getOrDefault(questionsFuture, List.of());

        if (intentExplanation == null && productTags.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new AiEnhancement(intentExplanation, productTags, marketAnalysis, suggestedQuestions));
    }

    private static <T> T getOrNull(CompletableFuture<T> future) {
        try {
            return future.getNow(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getOrDefault(CompletableFuture<?> future, T defaultValue) {
        try {
            Object result = ((CompletableFuture<Object>) future).getNow(null);
            return result != null ? (T) result : defaultValue;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
