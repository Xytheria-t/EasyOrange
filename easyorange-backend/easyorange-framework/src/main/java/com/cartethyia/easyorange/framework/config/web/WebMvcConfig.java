package com.cartethyia.easyorange.framework.config.web;

import com.cartethyia.easyorange.framework.config.properties.WebMvcProperties;
import com.cartethyia.easyorange.framework.handler.LoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;
    private final WebMvcProperties webMvcProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        var registration = registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(webMvcProperties.getExcludePaths().toArray(new String[0]));
        if (webMvcProperties.getInterceptorOrder() != 0) {
            registration.order(webMvcProperties.getInterceptorOrder());
        }
    }
}
