package com.cartethyia.easyorange.framework.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 业务指标服务 — 记录核心业务流程的 Metrics 数据，
 * 暴露给 Prometheus 抓取（/actuator/prometheus）。
 */
@Component
public class BusinessMetricsService {

    private final MeterRegistry meterRegistry;

    // ── 计数器 ──────────────────────────────────────────────────────
    private final Counter userRegistrationCounter;
    private final Counter productPublishedCounter;
    private final Counter orderCreatedCounter;
    private final Counter paymentCompletedCounter;
    private final Counter reportFiledCounter;

    // ── 仪表 ────────────────────────────────────────────────────────
    private final AtomicLong activeUsersGauge;

    // ── 计时器 ──────────────────────────────────────────────────────
    private final Timer orderProcessingTimer;

    public BusinessMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 注册计数器
        this.userRegistrationCounter = Counter.builder("easyorange.users.registered")
                .description("Total number of user registrations")
                .register(meterRegistry);

        this.productPublishedCounter = Counter.builder("easyorange.products.published")
                .description("Total number of products published")
                .register(meterRegistry);

        this.orderCreatedCounter = Counter.builder("easyorange.orders.created")
                .description("Total number of orders created")
                .register(meterRegistry);

        this.paymentCompletedCounter = Counter.builder("easyorange.payments.completed")
                .description("Total number of payments completed")
                .register(meterRegistry);

        this.reportFiledCounter = Counter.builder("easyorange.reports.filed")
                .description("Total number of reports filed")
                .register(meterRegistry);

        // 注册仪表 — 活跃用户数
        this.activeUsersGauge = new AtomicLong(0);
        io.micrometer.core.instrument.Gauge.builder("easyorange.users.active", activeUsersGauge, AtomicLong::get)
                .description("Current number of active users")
                .register(meterRegistry);

        // 注册计时器
        this.orderProcessingTimer = Timer.builder("easyorange.orders.processing.time")
                .description("Time taken to process an order")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    // ── 计数器方法 ──────────────────────────────────────────────────

    /** 用户注册 +1 */
    public void incrementUserRegistration() {
        userRegistrationCounter.increment();
    }

    /** 商品发布 +1 */
    public void incrementProductPublished() {
        productPublishedCounter.increment();
    }

    /** 订单创建 +1 */
    public void incrementOrderCreated() {
        orderCreatedCounter.increment();
    }

    /** 支付完成 +1 */
    public void incrementPaymentCompleted() {
        paymentCompletedCounter.increment();
    }

    /** 举报提交 +1 */
    public void incrementReportFiled() {
        reportFiledCounter.increment();
    }

    // ── 仪表方法 ────────────────────────────────────────────────────

    /** 设置活跃用户数 */
    public void setActiveUsers(long count) {
        activeUsersGauge.set(count);
    }

    // ── 计时器方法 ──────────────────────────────────────────────────

    /** 记录订单处理耗时 */
    public void recordOrderProcessingTime(long duration, TimeUnit unit) {
        orderProcessingTimer.record(duration, unit);
    }

    /** 获取订单处理计时器（适合 try-with-resources 或 lambda 采样） */
    public Timer getOrderProcessingTimer() {
        return orderProcessingTimer;
    }
}
