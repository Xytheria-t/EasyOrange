package com.cartethyia.easyorange.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.testsupport.PropertyBindings;
import com.cartethyia.easyorange.framework.config.cache.ImageProcessCacheConfig;
import com.cartethyia.easyorange.framework.config.properties.CacheProperties;
import com.cartethyia.easyorange.framework.file.service.ImageProcessingCacheEntry;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * stale 缓存装配 — 容器里两个 Caffeine 缓存（{@code aiStaleCache} 与 framework 的 {@code imageProcessCache}）
 * 靠值类型区分，注入点都不写 {@code @Qualifier}。这里把「泛型能唯一解析」断言化：探针注入点只写类型，
 * 一旦谁把值类型改回 {@code Object}（两个 bean 泛型撞车），容器 refresh 抛
 * {@code NoUniqueBeanDefinitionException} —— 本测试先红，不必等应用启动才发现。
 */
@DisplayName("Caffeine 缓存装配（stale 与 imageProcessCache 的泛型区分）")
class StaleCacheWiringTest {

    /** 探针 — 只声明注入类型，逼容器按泛型解析。 */
    @Configuration(proxyBeanMethods = false)
    static class ProbeConfig {

        @Bean
        ChatAnswerCacheProbe chatAnswerCacheProbe(Cache<String, ChatAnswer> cache) {
            return new ChatAnswerCacheProbe(cache);
        }

        @Bean
        ImageCacheProbe imageCacheProbe(Cache<String, ImageProcessingCacheEntry> cache) {
            return new ImageCacheProbe(cache);
        }
    }

    record ChatAnswerCacheProbe(Cache<String, ChatAnswer> cache) {}

    record ImageCacheProbe(Cache<String, ImageProcessingCacheEntry> cache) {}

    @Test
    @DisplayName("两个缓存并存 -> 按泛型各归其位，消费方无需 @Qualifier")
    void bothCachesResolveByGenericType() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AiProperties.class, () -> PropertyBindings.bind(AiProperties.class));
            context.registerBean(CacheProperties.class, () -> PropertyBindings.bind(CacheProperties.class));
            context.register(StaleCacheConfig.class, ImageProcessCacheConfig.class, ProbeConfig.class);
            context.refresh();

            // 前提前置：确实有两个同类型缓存，否则下面的断言等于没测
            assertThat(context.getBeansOfType(Cache.class)).hasSize(2);
            assertThat(context.getBean(ChatAnswerCacheProbe.class).cache()).isSameAs(context.getBean("aiStaleCache"));
            assertThat(context.getBean(ImageCacheProbe.class).cache()).isSameAs(context.getBean("imageProcessCache"));
        }
    }
}
