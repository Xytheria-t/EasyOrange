package com.cartethyia.easyorange.framework.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * WebMvc 日志拦截器注册配置。
 *
 * @param excludePaths 不注册日志拦截器的路径模式
 * @param skipLoggingPaths 注册但跳过日志的路径模式
 * @param interceptorOrder 拦截器顺序；0 表示沿用注册默认值，不显式指定
 */
@ConfigurationProperties(prefix = "webmvc")
public record WebMvcProperties(
        List<String> excludePaths,
        List<String> skipLoggingPaths,
        @DefaultValue("0") int interceptorOrder) {

    public WebMvcProperties {
        excludePaths = excludePaths == null ? List.of() : List.copyOf(excludePaths);
        skipLoggingPaths = skipLoggingPaths == null ? List.of() : List.copyOf(skipLoggingPaths);
    }
}
