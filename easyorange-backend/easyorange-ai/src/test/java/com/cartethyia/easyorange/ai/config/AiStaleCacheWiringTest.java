package com.cartethyia.easyorange.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.testsupport.PropertyBindings;
import com.cartethyia.easyorange.framework.config.cache.ImageProcessCacheConfig;
import com.cartethyia.easyorange.framework.config.properties.CacheProperties;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * stale 缓存装配 — 容器里同时存在两个 Caffeine 缓存（{@code aiStaleCache} 与 framework 的
 * {@code imageProcessCache}），靠值类型区分。
 * <p>
 * 这里把「泛型能唯一解析」断言化：两个探针的注入点只写类型、不写 {@code @Qualifier}，一旦两者的泛型
 * 撞回同一个（例如有人把 stale 缓存改回 {@code Cache<String, Object>}），容器 refresh 直接抛
 * {@code NoUniqueBeanDefinitionException}，这个测试先红 —— 否则要等应用启动才发现。
 */
@DisplayName("stale 缓存装配（与 imageProcessCache 的类型区分）")
class AiStaleCacheWiringTest {

    /** 探针 — 只声明注入类型，用构造注入逼容器解析。 */
    @Configuration(proxyBeanMethods = false)
    static class ProbeConfig {

        @Bean
        ChatAnswerCacheProbe chatAnswerCacheProbe(Cache<String, ChatAnswer> cache) {
            return new ChatAnswerCacheProbe(cache);
        }

        @Bean
        ImageCacheProbe imageCacheProbe(Cache<String, Object> cache) {
            return new ImageCacheProbe(cache);
        }
    }

    record ChatAnswerCacheProbe(Cache<String, ChatAnswer> cache) {}

    record ImageCacheProbe(Cache<String, Object> cache) {}

    @Test
    @DisplayName("两个缓存并存 -> 按泛型各归其位，消费方无需 @Qualifier")
    void bothCachesResolveByGenericType() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AiProperties.class, () -> PropertyBindings.bind(AiProperties.class));
            context.registerBean(CacheProperties.class, () -> PropertyBindings.bind(CacheProperties.class));
            context.register(AiStaleCacheConfig.class, ImageProcessCacheConfig.class, ProbeConfig.class);
            context.refresh();

            // 前提前置：确实有两个同名类型撞车，否则下面的断言等于没测
            assertThat(context.getBeansOfType(Cache.class)).hasSize(2);
            assertThat(context.getBean(ChatAnswerCacheProbe.class).cache()).isSameAs(context.getBean("aiStaleCache"));
            assertThat(context.getBean(ImageCacheProbe.class).cache()).isSameAs(context.getBean("imageProcessCache"));
        }
    }
}
