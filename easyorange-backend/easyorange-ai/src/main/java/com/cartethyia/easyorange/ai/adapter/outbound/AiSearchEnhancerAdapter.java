package com.cartethyia.easyorange.ai.adapter.outbound;

import com.cartethyia.easyorange.ai.port.LlmPort;
import com.cartethyia.easyorange.ai.service.NaturalLanguageDetector;
import com.cartethyia.easyorange.ai.service.ProductTagger;
import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.framework.cache.RedisCache;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.domain.port.AiSearchEnhancerPort;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class AiSearchEnhancerAdapter implements AiSearchEnhancerPort {

    private final NaturalLanguageDetector nlDetector;
    private final LlmPort llmPort;
    private final ProductTagger productTagger;
    private final RedisCache redisCache;

    private static final int TIMEOUT_SECONDS = 5;
    private static final long CACHE_TTL_MINUTES = 5;
    private static final String CACHE_KEY_PREFIX = "ai:search:enhance:";
    private static final int PARALLELISM = 4;
    private static final int TOP_PRODUCTS_LIMIT = 5;

    private static final String INTENT_SYSTEM_PROMPT = """
        你是 EasyOrange — AI 资产管理 的 AI 导购助手。
        用户输入了一段自然语言商品搜索需求。
        请用一句简洁的话总结用户想找什么，不超过30个字。
        直接输出总结，不要前缀。
        示例: "想找5000以内适合编程的笔记本"
        """;
    private static final String QUESTIONS_SYSTEM_PROMPT = """
        基于用户需求和搜索结果，生成2-3个用户可能想追问的问题。
        每个问题不超过15个字。
        用逗号分隔输出，不要序号。
        """;
    private static final String MARKET_SYSTEM_PROMPT = """
        你是 EasyOrange — AI 资产管理 的市场分析助手。根据搜索到的资产价格信息，
        用一句话概括当前市场价格情况（如均价、性价比等），不超过40个字。
        直接输出分析结果，不要前缀。
        """;

    private final ExecutorService executor = Executors.newFixedThreadPool(PARALLELISM, r -> {
        Thread t = new Thread(r, "ai-search-enhancer");
        t.setDaemon(true);
        return t;
    });

    public AiSearchEnhancerAdapter(
            NaturalLanguageDetector nlDetector,
            LlmPort llmPort,
            ProductTagger productTagger,
            ObjectProvider<RedisCache> redisCacheProvider) {
        this.nlDetector = nlDetector;
        this.llmPort = llmPort;
        this.productTagger = productTagger;
        this.redisCache = redisCacheProvider.getIfAvailable();
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
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

        if (redisCache != null) {
            try {
                AiEnhancement cached = redisCache.get(cacheKey, AiEnhancement.class);
                if (cached != null) {
                    log.debug("AI enhancement cache hit for keyword: {}", keyword);
                    return Optional.of(cached);
                }
            } catch (Exception e) {
                log.debug("Cache read failed for key {}: {}", cacheKey, e.getMessage());
            }
        }

        List<ProductReadModel> top5 = topProducts.subList(0, Math.min(TOP_PRODUCTS_LIMIT, topProducts.size()));

        CompletableFuture<String> intentFuture = CompletableFuture.supplyAsync(
            () -> llmPort.generateText(INTENT_SYSTEM_PROMPT, keyword), executor);

        CompletableFuture<Map<Long, List<String>>> tagsFuture = CompletableFuture.supplyAsync(
            () -> productTagger.tagProducts(top5), executor);

        String marketContext = buildMarketContext(top5);
        CompletableFuture<String> marketFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return llmPort.generateText(MARKET_SYSTEM_PROMPT, marketContext);
            } catch (Exception e) {
                log.warn("Market analysis failed", e);
                return null;
            }
        }, executor);

        CompletableFuture<List<String>> questionsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                String result = llmPort.generateText(QUESTIONS_SYSTEM_PROMPT, keyword);
                return result != null ? Arrays.asList(result.split("[,，]")) : List.of();
            } catch (Exception e) {
                log.warn("Suggested questions failed", e);
                return List.of();
            }
        }, executor);

        try {
            CompletableFuture.allOf(intentFuture, tagsFuture, marketFuture, questionsFuture)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("AI search enhancement timed out for keyword: {}", keyword);
            intentFuture.cancel(true);
            marketFuture.cancel(true);
            questionsFuture.cancel(true);
            return collectAndCache(cacheKey,
                collectPartialResults(intentFuture, tagsFuture, marketFuture, questionsFuture));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("AI search enhancement interrupted for keyword: {}", keyword);
            return Optional.empty();
        } catch (ExecutionException e) {
            log.warn("AI search enhancement failed for keyword: {}", keyword, e.getCause());
            return collectAndCache(cacheKey,
                collectPartialResults(intentFuture, tagsFuture, marketFuture, questionsFuture));
        }

        String intentExplanation = intentFuture.getNow(null);
        Map<Long, List<String>> productTags = tagsFuture.getNow(Map.of());
        String marketAnalysis = marketFuture.getNow(null);
        List<String> suggestedQuestions = questionsFuture.getNow(List.of());

        if (intentExplanation == null && productTags.isEmpty()) {
            return Optional.empty();
        }

        AiEnhancement result = new AiEnhancement(
            intentExplanation, productTags, marketAnalysis, suggestedQuestions
        );
        writeToCache(cacheKey, result);
        return Optional.of(result);
    }

    private Optional<AiEnhancement> collectAndCache(String cacheKey, Optional<AiEnhancement> result) {
        result.ifPresent(enhancement -> writeToCache(cacheKey, enhancement));
        return result;
    }

    private void writeToCache(String cacheKey, AiEnhancement enhancement) {
        if (redisCache != null) {
            try {
                redisCache.set(cacheKey, enhancement, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.debug("AI enhancement cached for key: {}", cacheKey);
            } catch (Exception e) {
                log.debug("Cache write failed for key {}: {}", cacheKey, e.getMessage());
            }
        }
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    private Optional<AiEnhancement> collectPartialResults(
            CompletableFuture<String> intentFuture,
            CompletableFuture<Map<Long, List<String>>> tagsFuture,
            CompletableFuture<String> marketFuture,
            CompletableFuture<List<String>> questionsFuture) {

        String intentExplanation = getOrNull(intentFuture);
        Map<Long, List<String>> productTags = getOrDefault(tagsFuture, Map.of());
        String marketAnalysis = getOrNull(marketFuture);
        List<String> suggestedQuestions = getOrDefault(questionsFuture, List.of());

        if (intentExplanation == null && productTags.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new AiEnhancement(
            intentExplanation, productTags, marketAnalysis, suggestedQuestions
        ));
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
