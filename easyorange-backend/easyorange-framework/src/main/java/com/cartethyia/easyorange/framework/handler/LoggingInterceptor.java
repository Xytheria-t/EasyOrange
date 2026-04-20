package com.cartethyia.easyorange.framework.handler;

import com.cartethyia.easyorange.common.util.RequestUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID = "traceId";
    private static final String START_TIME = "requestStartTime";

    private static final List<String> SKIP_LOGGING_PATTERNS = Arrays.asList(
            "/api/health",
            "/actuator/**"
    );
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final MeterRegistry meterRegistry;
    private final Counter requestCounter;
    private final Counter errorCounter;
    private final Timer requestTimer;

    public LoggingInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.requestCounter = Counter.builder("http.requests.total")
                .description("Total HTTP requests")
                .register(meterRegistry);
        
        this.errorCounter = Counter.builder("http.requests.errors")
                .description("Total HTTP errors")
                .register(meterRegistry);
        
        this.requestTimer = Timer.builder("http.requests.duration")
                .description("HTTP request duration")
                .register(meterRegistry);
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String uri = getPathPattern(request);
        if (shouldSkipLogging(uri)) {
            return true;
        }

        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME, startTime);

        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(TRACE_ID, traceId);
        MDC.put("clientIp", RequestUtil.getClientIp(request));
        MDC.put("method", request.getMethod());
        MDC.put("uri", uri);

        log.info("action=request traceId={} method={} uri={}", traceId, request.getMethod(), uri);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        String uri = getPathPattern(request);
        if (shouldSkipLogging(uri)) {
            return;
        }

        Long startTime = (Long) request.getAttribute(START_TIME);
        long costTime = System.currentTimeMillis() - startTime;

        int status = response.getStatus();
        String traceId = MDC.get(TRACE_ID);
        String method = request.getMethod();

        requestCounter.increment();
        requestTimer.record(costTime, TimeUnit.MILLISECONDS);

        if (ex != null || status >= 500) {
            errorCounter.increment();
            log.error("action=request_error traceId={} method={} uri={} status={} cost={}ms error={}",
                    traceId, method, uri, status, costTime, ex != null ? ex.getMessage() : "server_error");
            
            meterRegistry.counter("http.requests.errors", "uri", uri, "method", method, "status", String.valueOf(status))
                    .increment();
        } else if (status >= 400) {
            log.warn("action=request_warn traceId={} method={} uri={} status={} cost={}ms",
                    traceId, method, uri, status, costTime);
            
            meterRegistry.counter("http.requests.errors", "uri", uri, "method", method, "status", String.valueOf(status))
                    .increment();
        } else {
            log.info("action=request_end traceId={} status={} cost={}ms", traceId, status, costTime);
        }

        meterRegistry.timer("http.server.request", "uri", uri, "method", method)
                .record(costTime, TimeUnit.MILLISECONDS);

        MDC.clear();
    }

    private boolean shouldSkipLogging(String uri) {
        return SKIP_LOGGING_PATTERNS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    private String getPathPattern(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
