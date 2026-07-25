package com.cartethyia.easyorange.framework.mybatis;

import com.cartethyia.easyorange.framework.config.properties.SlowSqlProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 慢 SQL 检测拦截器。
 * <p>
 * 拦截 MyBatis Executor 的所有 query/update 操作，记录执行时间超过阈值的慢 SQL，
 * 并上报 Micrometer 指标（直方图 + P99 等百分位）。
 * <p>
 * 线程安全：拦截器实例由 MyBatis 单例持有，内部仅使用无状态局部变量。
 */
@Slf4j
@NullMarked
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
})
public class SlowSqlInterceptor implements Interceptor {

    private final SlowSqlProperties properties;
    private final Timer slowSqlTimer;
    private final Timer sqlTimer;

    public SlowSqlInterceptor(SlowSqlProperties properties, @Nullable MeterRegistry meterRegistry) {
        this.properties = properties;
        if (meterRegistry != null && properties.isMetricsEnabled()) {
            this.sqlTimer = Timer.builder("easyorange.sql.execution")
                    .description("SQL execution time (all queries)")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry);
            this.slowSqlTimer = Timer.builder("easyorange.sql.slow")
                    .description("Slow SQL execution time (exceeding threshold)")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry);
        } else {
            this.sqlTimer = null;
            this.slowSqlTimer = null;
        }
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            long elapsedNanos = System.nanoTime() - start;
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);

            // 上报全部 SQL 耗时指标
            if (sqlTimer != null) {
                sqlTimer.record(Duration.ofNanos(elapsedNanos));
            }

            // 慢查询检测
            if (properties.isEnabled() && elapsedMs >= properties.getThresholdMs()) {
                reportSlowSql(invocation, elapsedMs, elapsedNanos);
            }
        }
    }

    private void reportSlowSql(Invocation invocation, long elapsedMs, long elapsedNanos) {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        // 提取 SQL 信息
        String sql = getSql(ms, parameter);
        String namespace = ms.getId();
        String commandName = ms.getSqlCommandType().name();

        // 上报慢 SQL 指标
        if (slowSqlTimer != null) {
            slowSqlTimer.record(Duration.ofNanos(elapsedNanos));
        }

        // 结构化日志
        String message = String.format(
                "action=slow_sql namespace=%s command=%s cost=%dms threshold=%dms sql=[%s]",
                namespace, commandName, elapsedMs, properties.getThresholdMs(), sql
        );

        switch (properties.getLogLevel().toLowerCase(Locale.ROOT)) {
            case "trace" -> log.trace(message);
            case "debug" -> log.debug(message);
            case "info" -> log.info(message);
            case "error" -> log.error(message);
            default -> log.warn(message);
        }
    }

    /**
     * 从 MappedStatement + parameter 重建可读 SQL（含参数内联）。
     */
    private String getSql(MappedStatement ms, @Nullable Object parameter) {
        BoundSql boundSql = ms.getBoundSql(parameter);
        if (!properties.isLogParameters()) {
            return boundSql.getSql().replaceAll("\\s+", " ");
        }
        return buildSqlWithParams(ms.getConfiguration(), boundSql);
    }

    /**
     * 将参数内联到 SQL 中生成便于排查的完整 SQL 字符串。
     */
    private String buildSqlWithParams(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql().replaceAll("\\s+", " ");

        if (parameterMappings.isEmpty() || parameterObject == null) {
            return sql;
        }

        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        StringBuilder sb = new StringBuilder(sql.length() + 64);
        MetaObject metaObject = configuration.newMetaObject(parameterObject);

        int startIdx = 0;
        for (ParameterMapping mapping : parameterMappings) {
            String property = mapping.getProperty();
            int paramIdx = sql.indexOf("?", startIdx);
            if (paramIdx < 0) break;

            sb.append(sql, startIdx, paramIdx);
            startIdx = paramIdx + 1;

            Object value;
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                value = parameterObject;
            } else if (boundSql.hasAdditionalParameter(property)) {
                value = boundSql.getAdditionalParameter(property);
            } else if (property.startsWith("__frch_")) {
                // MyBatis-Plus / foreach 生成的参数
                value = metaObject.getValue(property);
            } else {
                value = metaObject.getValue(property);
            }

            sb.append(formatParam(value));
        }

        if (startIdx < sql.length()) {
            sb.append(sql, startIdx, sql.length());
        }

        return sb.toString();
    }

    private String formatParam(@Nullable Object value) {
        if (value == null) return "null";
        return switch (value) {
            case String s -> "'" + s + "'";
            case Date d -> DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(d);
            case Number ignored -> value.toString();
            case Boolean ignored -> value.toString();
            default -> "'" + value + "'";
        };
    }

    @Override
    public Object plugin(Object target) {
        // MyBatis 标准: 只拦截 Executor
        if (target instanceof Executor) {
            return Interceptor.super.plugin(target);
        }
        return target;
    }
}
