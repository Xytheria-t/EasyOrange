package com.cartethyia.easyorange.framework.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 业务指标服务 — 记录核心业务流程的 Metrics 数据，
 * 暴露给 Prometheus 抓取（/actuator/prometheus）。
 * <p>
 * 计数器由 {@code BusinessMetricsEventListener}（基于领域事件）和
 * 命令边界（举报提交）驱动，随业务动作递增。
 */
@Component
public class BusinessMetricsService {

    // ── 计数器 ──────────────────────────────────────────────────────
    private final Counter userRegistrationCounter;
    private final Counter productPublishedCounter;
    private final Counter orderCreatedCounter;
    private final Counter paymentCompletedCounter;
    private final Counter reportFiledCounter;

    public BusinessMetricsService(MeterRegistry meterRegistry) {

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
}
