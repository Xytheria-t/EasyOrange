package com.cartethyia.easyorange.framework.config.web;

import com.cartethyia.easyorange.framework.config.properties.FileUploadProperties;
import com.cartethyia.easyorange.framework.config.properties.WebMvcProperties;
import com.cartethyia.easyorange.framework.web.handler.LoggingInterceptor;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;
    private final WebMvcProperties webMvcProperties;
    private final FileUploadProperties fileUploadProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        var registration = registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(webMvcProperties.getExcludePaths().toArray(String[]::new));
        if (webMvcProperties.getInterceptorOrder() != 0) {
            registration.order(webMvcProperties.getInterceptorOrder());
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluteUploadPath = Paths.get(fileUploadProperties.getPath())
                .toAbsolutePath()
                .normalize()
                .toString();
        String urlPrefix = fileUploadProperties.getUrlPrefix();
        String urlPattern = urlPrefix.endsWith("/") ? urlPrefix + "**" : urlPrefix + "/**";

        registry.addResourceHandler(urlPattern).addResourceLocations("file:" + absoluteUploadPath + "/");

        log.info("静态资源映射已配置: {} -> file:{}/", urlPattern, absoluteUploadPath);
    }
}
