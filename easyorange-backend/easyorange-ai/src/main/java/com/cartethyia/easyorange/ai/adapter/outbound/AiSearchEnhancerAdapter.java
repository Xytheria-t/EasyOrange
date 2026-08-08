package com.cartethyia.easyorange.ai.adapter.outbound;

import com.cartethyia.easyorange.ai.adapter.outbound.tool.SearchTool;
import com.cartethyia.easyorange.ai.adapter.outbound.tool.SearchToolContext;
import com.cartethyia.easyorange.ai.adapter.outbound.tool.SearchToolRegistry;
import com.cartethyia.easyorange.ai.service.NaturalLanguageDetector;
import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.framework.cache.CacheUtils;
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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

/**
 * AI 导购搜索增强管道 — 4 路并行 Tool Calling（Tool Registry 模式）。
 * <p>
 * 用户自然语言查询进来，从 {@link SearchToolRegistry} 取 4 个工具并行执行：
 * intent_detection（LLM 意图识别）/ product_tagging（规则标签）/ market_analysis（LLM 市场分析）/
 * question_suggestion（LLM 建议问题）。单步骤失败降级不影响整体，5s 总超时，
 * 结果经 Redis 5min TTL 缓存。
 */
@Slf4j
@Primary
@Component
public class AiSearchEnhancerAdapter implements AiSearchEnhancerPort {

    private final NaturalLanguageDetector nlDetector;
    private final SearchToolRegistry toolRegistry;
    private final RedisTemplate<Object, Object> redisTemplate;

    private static final int TIMEOUT_SECONDS = 5;
    private static final long CACHE_TTL_MINUTES = 5;
    private static final String CACHE_KEY_PREFIX = "ai:search:enhance:";
    private static final int TOP_PRODUCTS_LIMIT = 5;

    private static final String TOOL_INTENT = "intent_detection";
    private static final String TOOL_TAGS = "product_tagging";
    private static final String TOOL_MARKET = "market_analysis";
    private static final String TOOL_QUESTIONS = "question_suggestion";

    public AiSearchEnhancerAdapter(
            NaturalLanguageDetector nlDetector,
            SearchToolRegistry toolRegistry,
            ObjectProvider<RedisTemplate<Object, Object>> redisTemplateProvider) {
        this.nlDetector = nlDetector;
        this.toolRegistry = toolRegistry;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    @Override
    public Optional<AiEnhancement> tryEnhance(String keyword, List<ProductReadModel> topProducts) {
        if (!nlDetector.isNaturalLanguage(keyword)) {
            return Optional.empty();
        }
        if (topProducts == null || topProducts.isEmpty()) {
            return Optional.empty();
        }

        String cacheKey = CACHE_KEY_PREFIX + md5(keyword);

        if (redisTemplate != null) {
            try {
                AiEnhancement cached =
                        CacheUtils.cast(redisTemplate.opsForValue().get(cacheKey), AiEnhancement.class);
                if (cached != null) {
                    log.debug("AI enhancement cache hit for keyword: {}", keyword);
                    return Optional.of(cached);
                }
            } catch (Exception e) {
                log.debug("Cache read failed for key {}: {}", cacheKey, e.getMessage());
            }
        }

        List<ProductReadModel> top5 = topProducts.subList(0, Math.min(TOP_PRODUCTS_LIMIT, topProducts.size()));
        var context = new SearchToolContext(keyword, top5, buildMarketContext(top5));

        CompletableFuture<String> intentFuture = runTool(TOOL_INTENT, context);
        CompletableFuture<Map<String, List<String>>> tagsFuture = runTool(TOOL_TAGS, context);
        CompletableFuture<String> marketFuture = runTool(TOOL_MARKET, context);
        CompletableFuture<List<String>> questionsFuture = runTool(TOOL_QUESTIONS, context);

        try {
            CompletableFuture.allOf(intentFuture, tagsFuture, marketFuture, questionsFuture)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("AI search enhancement timed out for keyword: {}", keyword);
            intentFuture.cancel(false);
            marketFuture.cancel(false);
            questionsFuture.cancel(false);
            return collectAndCache(
                    cacheKey, collectPartialResults(intentFuture, tagsFuture, marketFuture, questionsFuture));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("AI search enhancement interrupted for keyword: {}", keyword);
            return Optional.empty();
        } catch (ExecutionException e) {
            log.warn("AI search enhancement failed for keyword: {}", keyword, e.getCause());
            return collectAndCache(
                    cacheKey, collectPartialResults(intentFuture, tagsFuture, marketFuture, questionsFuture));
        }

        String intentExplanation = intentFuture.getNow(null);
        Map<String, List<String>> productTags = tagsFuture.getNow(Map.of());

        String marketAnalysis = marketFuture.getNow(null);
        List<String> suggestedQuestions = questionsFuture.getNow(List.of());

        if (intentExplanation == null && productTags.isEmpty()) {
            return Optional.empty();
        }

        AiEnhancement result = new AiEnhancement(intentExplanation, productTags, marketAnalysis, suggestedQuestions);
        writeToCache(cacheKey, result);
        return Optional.of(result);
    }

    /**
     * 从注册表取工具并提交并行执行；工具内部已处理各自降级（LLM 工具捕获异常返回降级值）。
     */
    @SuppressWarnings("unchecked")
    private <T> CompletableFuture<T> runTool(String name, SearchToolContext context) {
        return (CompletableFuture<T>) toolRegistry.get(name).run(context);
    }

    private Optional<AiEnhancement> collectAndCache(String cacheKey, Optional<AiEnhancement> result) {
        result.ifPresent(enhancement -> writeToCache(cacheKey, enhancement));
        return result;
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

    private String buildMarketContext(List<ProductReadModel> products) {
        var sb = new StringBuilder("搜索到以下商品价格:\n");
        for (var p : products) {
            sb.append(String.format("- %s: ¥%s", p.title(), p.price()));
            if (p.originalPrice() != null) {
                sb.append(String.format("(原价¥%s)", p.originalPrice()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
