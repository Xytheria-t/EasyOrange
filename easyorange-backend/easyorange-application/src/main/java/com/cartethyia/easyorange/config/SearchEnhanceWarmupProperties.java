package com.cartethyia.easyorange.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 搜索增强缓存预热配置（录屏用）。
 * <p>
 * dev 开、prod 默认关；{@code queries} 用 YAML 列表配（{@code @Value} 绑不了列表），
 * 热词由预热器运行时自己取，不进配置。
 *
 * @param enabled       是否启用预热（关闭 = 启动与定时都不触发）
 * @param queries       预热查询串
 * @param fixedDelayMs  定时刷新间隔（毫秒），必须小于增强缓存 TTL 才有「任意时刻都热」的效果
 */
@Validated
@ConfigurationProperties(prefix = "easyorange.ai.search-enhance.warmup")
public record SearchEnhanceWarmupProperties(
        @DefaultValue("false") boolean enabled,
        List<String> queries,
        @DefaultValue("240000") long fixedDelayMs) {

    public SearchEnhanceWarmupProperties {
        queries = List.copyOf(queries == null ? List.of() : queries);
    }
}
