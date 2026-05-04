package com.cartethyia.easyorange.framework.config.web;

import com.cartethyia.easyorange.framework.config.properties.WebMvcProperties;
import com.cartethyia.easyorange.framework.handler.LoggingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;
    private final WebMvcProperties webMvcProperties;

    @Value("${file.upload.path:./upload}")
    private String uploadPath;

    @Value("${file.upload.url-prefix:/api/file/}")
    private String urlPrefix;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        var registration = registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(webMvcProperties.getExcludePaths().toArray(new String[0]));
        if (webMvcProperties.getInterceptorOrder() != 0) {
            registration.order(webMvcProperties.getInterceptorOrder());
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluteUploadPath = Paths.get(uploadPath).toAbsolutePath().normalize().toString();
        String urlPattern = urlPrefix.endsWith("/") ? urlPrefix + "**" : urlPrefix + "/**";

        registry.addResourceHandler(urlPattern)
                .addResourceLocations("file:" + absoluteUploadPath + "/");

        log.info("静态资源映射已配置: {} -> file:{}/", urlPattern, absoluteUploadPath);
    }
}
