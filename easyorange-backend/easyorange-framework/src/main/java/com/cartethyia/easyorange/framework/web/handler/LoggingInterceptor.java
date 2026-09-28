package com.cartethyia.easyorange.framework.web.handler;

import com.cartethyia.easyorange.framework.config.properties.WebMvcProperties;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@NullMarked
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "requestStartTime";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final MeterRegistry meterRegistry;
    private final List<String> skipLoggingPaths;

    public LoggingInterceptor(MeterRegistry meterRegistry, WebMvcProperties webMvcProperties) {
        this.meterRegistry = meterRegistry;
        this.skipLoggingPaths = webMvcProperties.skipLoggingPaths();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
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
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) {
        var uri = request.getRequestURI();
        if (shouldSkipLogging(uri)) return;

        var costTime = System.currentTimeMillis() - (long) request.getAttribute(START_TIME);
        var status = response.getStatus();
        var method = request.getMethod();

        // 只留无 tag 的总请求计数：actuator 的 http.server.requests 已按路径模板给出耗时/状态/异常分维度，
        // 这里再挂一个 timer 只会用原始 URI（含 {id} 路径变量）开新时间序列，无基数上限且信息量严格更少
        meterRegistry.counter("http.requests.total").increment();

        if (ex != null || status >= 500) {
            log.error(
                    "action=request_error method={} uri={} status={} cost={}ms error={}",
                    method,
                    uri,
                    status,
                    costTime,
                    ex != null ? ex.getMessage() : "server_error");
        } else if (status >= 400) {
            log.warn("action=request_warn method={} uri={} status={} cost={}ms", method, uri, status, costTime);
        }

        MDC.clear();
    }

    private boolean shouldSkipLogging(String uri) {
        return skipLoggingPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
}
