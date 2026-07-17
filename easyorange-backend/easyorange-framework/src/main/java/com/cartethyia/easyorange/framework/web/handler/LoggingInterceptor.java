package com.cartethyia.easyorange.framework.web.handler;

import com.cartethyia.easyorange.framework.util.RequestUtil;
import com.cartethyia.easyorange.framework.config.properties.WebMvcProperties;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "requestStartTime";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final MeterRegistry meterRegistry;
    private final List<String> skipLoggingPaths;

    public LoggingInterceptor(MeterRegistry meterRegistry, WebMvcProperties webMvcProperties) {
        this.meterRegistry = meterRegistry;
        this.skipLoggingPaths = webMvcProperties.getSkipLoggingPaths();
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        var uri = request.getRequestURI();
        if (shouldSkipLogging(uri)) return true;

        request.setAttribute(START_TIME, System.currentTimeMillis());

        // traceId 由 Micrometer Tracing (Brave) 自动注入 MDC，无需手写
        MDC.put("clientIp", RequestUtil.getClientIp(request));
        MDC.put("method", request.getMethod());
        MDC.put("uri", uri);
        MDC.put("fullUrl", RequestUtil.getFullRequestUrl(request));

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        var uri = request.getRequestURI();
        if (shouldSkipLogging(uri)) return;

        var costTime = System.currentTimeMillis() - (long) request.getAttribute(START_TIME);
        var status = response.getStatus();
        var method = request.getMethod();

        meterRegistry.counter("http.requests.total").increment();

        if (ex != null || status >= 500) {
            log.error("action=request_error method={} uri={} status={} cost={}ms error={}",
                    method, uri, status, costTime, ex != null ? ex.getMessage() : "server_error");
        } else if (status >= 400) {
            log.warn("action=request_warn method={} uri={} status={} cost={}ms",
                    method, uri, status, costTime);
        }

        meterRegistry.timer("http.server.request", "uri", uri, "method", method)
                .record(costTime, TimeUnit.MILLISECONDS);

        MDC.clear();
    }

    private boolean shouldSkipLogging(String uri) {
        return skipLoggingPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
}
