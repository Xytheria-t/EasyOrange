package com.cartethyia.easyorange.adapter.inbound.job;

import com.cartethyia.easyorange.config.SearchEnhanceWarmupProperties;
import com.cartethyia.easyorange.product.application.port.query.AiSearchEnhancerPort;
import com.cartethyia.easyorange.product.application.query.ProductSearchCriteria;
import com.cartethyia.easyorange.product.application.query.ProductSearchQueryHandler;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 搜索增强缓存预热 — 录屏场景下「预热」不能依赖人工临场先搜一次。
 * <p>
 * 意图识别 LLM 的冷首查 2~3s、命中 Redis 后的热查亚秒级，差距全在增强缓存
 * （{@code ai:search:enhance:*}，TTL 5min）上。演示是录屏，冷启动后的第一查就应当是热查，
 * 所以这里在启动后异步跑一轮，并按固定间隔（默认 4min &lt; TTL）持续刷新。
 * <p>
 * 三条 fail-open 约束，任意一条不满足都只是少一次预热，绝不影响启动与检索：
 * <ul>
 *   <li>配置关闭（{@code enabled=false}，prod 默认）或未配文本模型 key → 整段跳过，不打模型不烧额度；
 *   <li>预热跑在 {@code applicationTaskExecutor}（虚拟线程）上，不阻塞 readiness；
 *   <li>单条查询任何异常只记 warn，关键词级去重保证同一 key 不会并发重复打模型。
 * </ul>
 * 刷新走 {@link AiSearchEnhancerPort#refresh} 而非普通增强：命中式预热在 TTL &gt; 刷新间隔时
 * 会留下「已过期但未到下一次刷新」的空窗，强制重算才能真正做到任意时刻录制都命中热缓存。
 */
@Slf4j
@Component
public class AiSearchEnhancementWarmup implements ApplicationRunner {

    /** 热词只取前 5 个：再多只是给冷门词白付模型调用，热词本身边缘化很快。 */
    private static final int HOT_KEYWORD_LIMIT = 5;

    /** 增强只消费 top5（与 {@code ProductSearchQueryHandler} 内增强口径一致），取够 5 条即可。 */
    private static final int WARMUP_PAGE_SIZE = 5;

    private final ProductSearchQueryHandler searchQueryHandler;
    private final AiSearchEnhancerPort enhancer;
    private final Executor executor;
    private final boolean enabled;
    private final List<String> configuredQueries;
    private final String chatApiKey;
    private final Set<String> warming = ConcurrentHashMap.newKeySet();

    public AiSearchEnhancementWarmup(
            ProductSearchQueryHandler searchQueryHandler,
            ObjectProvider<AiSearchEnhancerPort> enhancerProvider,
            @Qualifier("applicationTaskExecutor") ObjectProvider<TaskExecutor> taskExecutors,
            SearchEnhanceWarmupProperties properties,
            @Value("${easyorange.ai.deepseek.api-key:}") String chatApiKey) {
        this.searchQueryHandler = searchQueryHandler;
        this.enhancer = enhancerProvider.getIfAvailable();
        var taskExecutor = taskExecutors.getIfAvailable();
        // 取不到执行器（极简上下文）时退回当前线程：预热失败也只影响预热本身
        this.executor = taskExecutor == null ? Runnable::run : taskExecutor;
        this.enabled = properties.enabled();
        this.configuredQueries = properties.queries();
        this.chatApiKey = chatApiKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        trigger("startup");
    }

    @Scheduled(
            initialDelayString = "${easyorange.ai.search-enhance.warmup.fixed-delay-ms:240000}",
            fixedDelayString = "${easyorange.ai.search-enhance.warmup.fixed-delay-ms:240000}")
    public void scheduledRefresh() {
        trigger("scheduled");
    }

    private void trigger(String reason) {
        if (!enabled) {
            return;
        }
        if (enhancer == null) {
            log.info("action=search_enhance_warmup_skip, trigger={}, reason=no_enhancer", reason);
            return;
        }
        if (chatApiKey == null || chatApiKey.isBlank()) {
            log.info("action=search_enhance_warmup_skip, trigger={}, reason=no_ai_key", reason);
            return;
        }
        executor.execute(() -> warmAll(reason));
    }

    private void warmAll(String reason) {
        long startMs = System.currentTimeMillis();
        int enhanced = 0;
        // 配置查询串走完整链路（含语义召回），热词只跑增强那一段
        for (String keyword : configuredQueries) {
            if (hasText(keyword) && warmOne(keyword, true)) {
                enhanced++;
            }
        }
        for (String keyword : hotKeywords()) {
            if (warmOne(keyword, false)) {
                enhanced++;
            }
        }
        log.info(
                "action=search_enhance_warmup, trigger={}, enhanced={}, cost_ms={}",
                reason,
                enhanced,
                System.currentTimeMillis() - startMs);
    }

    private List<String> hotKeywords() {
        try {
            return searchQueryHandler.getHotKeywords(HOT_KEYWORD_LIMIT).stream()
                    .map(HotKeywordReadModel::keyword)
                    .filter(AiSearchEnhancementWarmup::hasText)
                    .filter(keyword -> !configuredQueries.contains(keyword))
                    .toList();
        } catch (Exception e) {
            log.warn("热词读取失败，仅预热配置查询串", e);
            return List.of();
        }
    }

    private boolean warmOne(String keyword, boolean aiEnhanced) {
        if (!warming.add(keyword)) {
            log.debug("action=search_enhance_warmup_skip, keyword={}, reason=in_flight", keyword);
            return false;
        }
        try {
            // forceRefresh=true：忽略已有缓存重算并重置 TTL，否则「写完 5min 后过期、下一次刷新还没到」
            // 的空窗会让录屏第一查撞上冷路径。热词是短词面查询，BM25 本就够，不挂 AI 省一次 embedding
            var result = searchQueryHandler.search(criteria(keyword), aiEnhanced, true);
            if (result.aiEnhancement() == null) {
                log.info(
                        "action=search_enhance_warmup_miss, keyword={}, degraded={}, hits={}",
                        keyword,
                        result.aiEnhancementDegraded(),
                        result.page().total());
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("搜索增强预热失败，不影响启动与检索, keyword={}", keyword, e);
            return false;
        } finally {
            warming.remove(keyword);
        }
    }

    private static ProductSearchCriteria criteria(String keyword) {
        return new ProductSearchCriteria(keyword, null, null, null, null, null, null, null, 1, WARMUP_PAGE_SIZE);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
