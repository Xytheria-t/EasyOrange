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
 * stale 缓存装配 — 容器里同时存在两个 Caffeine 缓存（{@code aiStaleCache} 与 framework 的
 * {@code imageProcessCache}），两者靠<b>值类型</b>区分，注入点都不写 {@code @Qualifier}。
 * <p>
 * 这里把「泛型能唯一解析」断言化：探针的注入点只写类型，一旦有人把任一缓存的值类型改回
 * {@code Object}（两个 bean 泛型撞车），容器 refresh 直接抛
 * {@code NoUniqueBeanDefinitionException}，或该注入点解析不到 bean —— 这个测试先红，
 * 否则要等应用启动才发现。
 */
@DisplayName("Caffeine 缓存装配（stale 与 imageProcessCache 的泛型区分）")
class AiStaleCacheWiringTest {

    /** 探针 — 只声明注入类型，用构造注入逼容器解析。 */
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
            context.register(AiStaleCacheConfig.class, ImageProcessCacheConfig.class, ProbeConfig.class);
            context.refresh();

            // 前提前置：确实有两个同类型缓存，否则下面的断言等于没测
            assertThat(context.getBeansOfType(Cache.class)).hasSize(2);
            assertThat(context.getBean(ChatAnswerCacheProbe.class).cache()).isSameAs(context.getBean("aiStaleCache"));
            assertThat(context.getBean(ImageCacheProbe.class).cache()).isSameAs(context.getBean("imageProcessCache"));
        }
    }
}
