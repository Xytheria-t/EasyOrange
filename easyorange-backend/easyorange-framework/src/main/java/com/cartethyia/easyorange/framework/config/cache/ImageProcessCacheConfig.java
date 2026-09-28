package com.cartethyia.easyorange.framework.config.cache;

import com.cartethyia.easyorange.framework.config.properties.CacheProperties;
import com.cartethyia.easyorange.framework.file.service.ImageProcessingCacheEntry;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 图片处理本地缓存配置 — Caffeine（进程内），独立于 Redis 缓存（{@link RedisCacheConfig}）。
 * <p>
 * 值类型是 {@link ImageProcessingCacheEntry}：缓存 bean 的泛型因此唯一，注入点按类型即可解析，
 * 消费方（{@code ImageQueryService}）不必用 {@code @Qualifier} 字符串限定，也不必对读到的值强转。
 */
@Slf4j
@AutoConfiguration
public class ImageProcessCacheConfig {

    private final CacheProperties cacheProperties;

    public ImageProcessCacheConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * 条目一旦被驱逐（超容量 / 过期 / 显式 evict）就删掉它落盘的处理结果 ——
     * 缓存条目只持有路径，文件不会随条目一起消失；不挂这个监听器，24h 过期后磁盘文件永久残留，
     * {@code deleteOnExit} 还会把每个文件永久登记进 JVM 的退出钩子集合，长跑进程持续涨堆。
     * 删除失败只记日志：此时唯一线索就是日志，抛出去反而给缓存操作加上异常语义。
     */
    @Bean("imageProcessCache")
    @ConditionalOnMissingBean(name = "imageProcessCache")
    public Cache<String, ImageProcessingCacheEntry> imageProcessCache() {
        var imageProps = cacheProperties.image();
        return Caffeine.<String, ImageProcessingCacheEntry>newBuilder()
                .maximumSize(imageProps.maxSize())
                .expireAfterAccess(imageProps.expireHours(), TimeUnit.HOURS)
                .recordStats()
                // Caffeine 的 removalListener 形参是 RemovalListener<? super K, ? super V>，
                // 泛型实参不落到 lambda 形参上（javac 推成 Object），故三个参数显式标注类型
                .removalListener((String key, ImageProcessingCacheEntry value, RemovalCause cause) -> {
                    if (value == null) return;
                    try {
                        Files.deleteIfExists(value.file().toPath());
                    } catch (IOException e) {
                        log.warn("action=image_cache_file_delete_failed, key={}, cause={}", key, cause, e);
                    }
                })
                .build();
    }
}
