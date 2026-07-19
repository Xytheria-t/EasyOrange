package com.cartethyia.easyorange.framework.event.core;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.event.metadata.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;

/**
 * 领域事件消费者模板基类。
 * <p>
 * 模板方法 {@link #handle(DomainEvent, Message)} 统一封装：
 * <ol>
 *   <li>事件元数据解码（{@link EventMetadata#from(Message, DomainEvent)}）</li>
 *   <li>幂等性检查（{@link EventIdempotencyChecker}，可关闭）</li>
 *   <li>Micrometer 指标埋点（接收计数 + 处理时延）</li>
 *   <li>结构化日志（开始 / 完成 / 失败）</li>
 *   <li>异常透传（交给 {@code RetryTemplate} 重试或进 DLQ）</li>
 * </ol>
 * <p>
 * 子类只需：
 * <ul>
 *   <li>实现 {@link #doHandle(DomainEvent, EventMetadata)} 业务逻辑</li>
 *   <li>用 {@code @RabbitHandler} 注解单行委托方法调用 {@link #handle(DomainEvent, Message)}</li>
 *   <li>对不需要幂等的消费者，构造器传 {@code idempotencyEnabled=false}</li>
 * </ol>
 */
public abstract class AbstractDomainEventConsumer {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final EventIdempotencyChecker idempotencyChecker;
    private final EventMetricsService metricsService;
    private final boolean idempotencyEnabled;

    /**
     * 默认启用幂等检查。
     */
    protected AbstractDomainEventConsumer(EventIdempotencyChecker idempotencyChecker,
                                           EventMetricsService metricsService) {
        this(idempotencyChecker, metricsService, true);
    }

    /**
     * @param idempotencyEnabled 通知类/指标类消费者可传 false 跳过幂等检查
     */
    protected AbstractDomainEventConsumer(EventIdempotencyChecker idempotencyChecker,
                                           EventMetricsService metricsService,
                                           boolean idempotencyEnabled) {
        this.idempotencyChecker = idempotencyChecker;
        this.metricsService = metricsService;
        this.idempotencyEnabled = idempotencyEnabled;
    }

    /**
     * 模板方法：幂等检查 → metrics → 业务处理 → 异常透传。
     * <p>
     * 子类的 {@code @RabbitHandler} 方法只需单行委托：{@code handle(event, message);}。
     */
    public final void handle(DomainEvent event, Message message) {
        var metadata = EventMetadata.from(message, event);
        var sample = metricsService.startTimer();
        var eventType = event.eventType();
        var aggregateId = event.aggregateId();
        var outcome = "success";

        try {
            if (idempotencyEnabled && isDuplicate(event)) {
                log.info("事件重复跳过: type={} aggregateId={} key={} consumer={}",
                        eventType, aggregateId, event.idempotencyKey(), consumerId());
                metricsService.recordReceived(eventType, "duplicate");
                return;
            }
            log.info("事件处理开始: type={} aggregateId={} eventId={} traceId={} consumer={}",
                    eventType, aggregateId, metadata.eventId(), metadata.traceId(), consumerId());
            doHandle(event, metadata);
            log.info("事件处理完成: type={} aggregateId={} consumer={}",
                    eventType, aggregateId, consumerId());
        } catch (Exception e) {
            outcome = "failure";
            log.error("事件处理失败: type={} aggregateId={} eventId={} consumer={}",
                    eventType, aggregateId, metadata.eventId(), consumerId(), e);
            throw e;
        } finally {
            metricsService.recordReceived(eventType, outcome);
            metricsService.recordDuration(eventType, sample, outcome);
        }
    }

    /**
     * 子类实现具体业务逻辑。
     */
    protected abstract void doHandle(DomainEvent event, EventMetadata metadata);

    /**
     * 消费者标识 — 用于幂等键命名空间隔离。
     * <p>
     * 默认返回子类 simple name。多个消费者监听同一事件时（如 OrderSagaEventConsumer
     * 与 OrderNotificationEventConsumer 都监听 OrderCreatedEvent），各自有独立的幂等键，
     * 不会互相阻塞。子类可重写为更短的标识。
     */
    protected String consumerId() {
        return getClass().getSimpleName();
    }

    /**
     * 幂等检查：用 consumerId + eventType 作为 Redis eventType tag，
     * 同一事件可被多个消费者独立处理。先查已处理标记，未命中则尝试加锁标记。
     * 并发场景下未抢到锁返回 true（视为重复，跳过本次处理）。
     */
    private boolean isDuplicate(DomainEvent event) {
        var redisEventType = consumerId() + ":" + event.eventType();
        var key = event.idempotencyKey();
        if (idempotencyChecker.isDuplicate(redisEventType, key)) {
            return true;
        }
        return !idempotencyChecker.tryMark(redisEventType, key);
    }
}
