package com.cartethyia.easyorange.framework.event.dlq;

import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DLQ 分级重试调度器 — 定时从 DLQ 拉取死信消息，按指数退避重投主队列或转储 terminal 队列。
 * <p>
 * 三级重试阶梯：
 * <ol>
 *   <li>主队列 RetryTemplate 快速重试（{@code maxAttempts=3}，指数退避 1s 起、10s 封顶）</li>
 *   <li>消息进入 DLQ → 本调度器每 5 分钟（{@code fixedDelay=300000}）扫描，按 {@code x-retry-count}
 *       指数退避（1/5/15 分钟，自本次死信时间起算）重投主交换（原 routing key），{@code x-retry-count + 1}</li>
 *   <li>重试次数 ≥ {@code MAX_RETRIES}(3) → 转储 {@code eo.dlq.terminal} 队列，等待人工介入</li>
 * </ol>
 * <p>
 * 实现要点：
 * <ul>
 *   <li>退避未到期的消息不重投，以 {@code basicNack(requeue=true)} 留在 DLQ 等待下一轮扫描评估；
 *       实际重投间隔为「退避值向上取整到扫描周期」（5 分钟扫描粒度）</li>
 *   <li>手动 ack（{@code basicGet(autoAck=false)} + 处理成功后 {@code basicAck}）：调度器在处理中途崩溃时，
 *       消息由 RabbitMQ 连接关闭自动回队，杜绝「receive 后、重投前崩溃丢消息」窗口</li>
 *   <li>幂等安全：重投消息保留原 body + eventId，消费者端
 *       {@link com.cartethyia.easyorange.framework.event.core.EventConsumerHandler} 基于 eventId 去重，
 *       重复投递不会产生副作用</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DlqRetryScheduler {

    private static final String X_RETRY_COUNT_HEADER = "x-retry-count";
    private static final String X_DEATH_HEADER = "x-death";
    private static final String X_DEATH_ROUTING_KEYS = "routing-keys";

    /** 退避时间窗（分钟）：x-retry-count=0/1/2 分别等待 1/5/15 分钟（自本次死信时间起算） */
    private static final long[] BACKOFF_DELAYS_MINUTES = {1, 5, 15};

    /** 单队列单次扫描最多拉取条数（包内可见供测试断言批次上限） */
    static final int BATCH_SIZE = 10;
    private static final int MAX_RETRIES = 3;

    /** 所有需要扫描的 DLQ 队列（与 RabbitMQConfig 中声明的主队列一一对应） */
    private static final List<String> DLQ_QUEUES = List.of(
            RabbitMQConfig.QUEUE_PRODUCT_CQRS + ".dlq",
            RabbitMQConfig.QUEUE_ORDER_NOTIFICATION + ".dlq",
            RabbitMQConfig.QUEUE_ORDER_LIFECYCLE + ".dlq",
            RabbitMQConfig.QUEUE_AUDIT_NOTIFICATION + ".dlq",
            RabbitMQConfig.QUEUE_AUDIT_LOG + ".dlq",
            RabbitMQConfig.QUEUE_REPORT_NOTIFICATION + ".dlq",
            RabbitMQConfig.QUEUE_MESSAGE_WEBSOCKET + ".dlq",
            RabbitMQConfig.QUEUE_PAYMENT_METRICS + ".dlq",
            RabbitMQConfig.QUEUE_AI_PRODUCT + ".dlq",
            RabbitMQConfig.QUEUE_AI_CREDIT + ".dlq");

    private final RabbitTemplate rabbitTemplate;
    private final EventMetricsService metricsService;

    /** 把 basicGet 返回的 AMQP 属性还原为 Spring MessageProperties（与 RabbitTemplate 默认转换器一致） */
    private static final MessagePropertiesConverter PROPERTIES_CONVERTER = new DefaultMessagePropertiesConverter();

    /**
     * 每 5 分钟扫描所有 DLQ 队列，拉取死信消息重投或转储。
     * <p>
     * fixedDelay=300000 确保上次执行完成后等待 5 分钟再开始下一次，避免并发扫描重叠。
     */
    @Scheduled(fixedDelay = 300_000)
    public void retryFromDlq() {
        for (String dlqQueue : DLQ_QUEUES) {
            try {
                int processed = processQueue(dlqQueue);
                if (processed > 0) {
                    log.info("action=dlq_retry_batch, queue={}, processed={}", dlqQueue, processed);
                }
            } catch (Exception e) {
                log.error("action=dlq_retry_scan_failed, queue={}", dlqQueue, e);
            }
        }
    }

    /**
     * 扫描单个 DLQ 队列：手动 ack 语义下逐条 {@code basicGet}，处理成功 ack、失败或退避未到期回队，
     * 单次最多处理 {@value #BATCH_SIZE} 条。
     */
    private int processQueue(String dlqQueue) throws Exception {
        return rabbitTemplate.execute(channel -> {
            int processed = 0;
            while (processed < BATCH_SIZE) {
                GetResponse response = channel.basicGet(dlqQueue, false);
                if (response == null) {
                    break;
                }
                long deliveryTag = response.getEnvelope().getDeliveryTag();
                Message message = toSpringMessage(response);
                try {
                    if (processMessage(dlqQueue, message) == RetryAction.WAIT) {
                        // 退避未到期：回队等待，由下一轮扫描重新评估
                        channel.basicNack(deliveryTag, false, true);
                    } else {
                        channel.basicAck(deliveryTag, false);
                    }
                } catch (Exception e) {
                    log.error("action=dlq_message_process_failed, queue={}", dlqQueue, e);
                    // 处理失败回队，手动 ack 语义下无丢失窗口
                    channel.basicNack(deliveryTag, false, true);
                }
                processed++;
            }
            return processed;
        });
    }

    /** 把 {@code basicGet} 返回的 AMQP 属性经标准转换器还原为 Spring {@link Message}（含 x-death 等头）。 */
    private Message toSpringMessage(GetResponse response) {
        return new Message(
                response.getBody(),
                PROPERTIES_CONVERTER.toMessageProperties(response.getProps(), response.getEnvelope(), "UTF-8"));
    }

    /** 单条死信消息的处置结果：重投主队列 / 转储 terminal / 退避未到期回队等待 */
    private enum RetryAction {
        REPUBLISH,
        TERMINAL,
        WAIT
    }

    private RetryAction processMessage(String dlqQueue, Message message) {
        String originalQueue = dlqQueue.replace(".dlq", "");
        int retryCount = getRetryCount(message);
        String routingKey = extractOriginalRoutingKey(message);

        if (routingKey == null) {
            log.warn("action=dlq_terminal_no_routing_key, queue={}, retryCount={}", originalQueue, retryCount);
            moveToTerminal(dlqQueue, message, "no-routing-key");
            metricsService.recordDlq(originalQueue, "terminal_no_routing_key");
            return RetryAction.TERMINAL;
        }

        if (retryCount >= MAX_RETRIES) {
            log.warn(
                    "action=dlq_terminal_max_retries, queue={}, retryCount={}, maxRetries={}",
                    originalQueue,
                    retryCount,
                    MAX_RETRIES);
            moveToTerminal(dlqQueue, message, "max-retries");
            metricsService.recordDlq(originalQueue, "terminal_max_retries");
            return RetryAction.TERMINAL;
        }

        if (!isBackoffDue(message, retryCount)) {
            log.info("action=dlq_backoff_wait, queue={}, retryCount={}", originalQueue, retryCount);
            metricsService.recordDlq(originalQueue, "backoff_wait");
            return RetryAction.WAIT;
        }

        log.info("action=dlq_retry, queue={}, retryCount={}, routingKey={}", originalQueue, retryCount, routingKey);
        republishToMainExchange(message, routingKey, retryCount, originalQueue);
        metricsService.recordDlq(originalQueue, "retry");
        return RetryAction.REPUBLISH;
    }

    /**
     * 退避是否到期：自本次死信时间（{@code x-death} 首条记录的 time）起，等待
     * {@code BACKOFF_DELAYS_MINUTES[retryCount]} 分钟。无死信时间可参考（手工投递等）时不额外等待。
     */
    private boolean isBackoffDue(Message message, int retryCount) {
        var headers = message.getMessageProperties().getHeaders();
        var xDeath = (List<Map<String, Object>>) headers.get(X_DEATH_HEADER);
        if (xDeath == null || xDeath.isEmpty()) {
            return true;
        }
        Object time = xDeath.get(0).get("time");
        long deathMillis = switch (time) {
            case null -> -1;
            case Number n -> n.longValue();
            case Date d -> d.getTime();
            default -> -1;
        };
        if (deathMillis < 0) {
            return true;
        }
        long delayMillis = TimeUnit.MINUTES.toMillis(BACKOFF_DELAYS_MINUTES[retryCount]);
        return System.currentTimeMillis() >= deathMillis + delayMillis;
    }

    // ───────────────────────── Republish helpers ─────────────────────────

    private void republishToMainExchange(
            Message message, String routingKey, int currentRetryCount, String originalQueue) {
        var props = message.getMessageProperties();
        props.setHeader(X_RETRY_COUNT_HEADER, currentRetryCount + 1);
        rabbitTemplate.send(RabbitMQConfig.EXCHANGE_NAME, routingKey, message);
    }

    private void moveToTerminal(String dlqQueue, Message message, String reason) {
        var props = message.getMessageProperties();
        props.setHeader("x-terminal-reason", reason);
        props.setHeader("x-source-dlq", dlqQueue);
        rabbitTemplate.send(RabbitMQConfig.TERMINAL_QUEUE, message);
    }

    // ───────────────────────── Header extraction ─────────────────────────

    @SuppressWarnings("unchecked")
    private String extractOriginalRoutingKey(Message message) {
        var headers = message.getMessageProperties().getHeaders();
        var xDeath = (List<Map<String, Object>>) headers.get(X_DEATH_HEADER);
        if (xDeath == null || xDeath.isEmpty()) {
            return null;
        }
        var firstEntry = xDeath.get(0);
        var routingKeys = (List<String>) firstEntry.get(X_DEATH_ROUTING_KEYS);
        if (routingKeys == null || routingKeys.isEmpty()) {
            return null;
        }
        return routingKeys.get(0);
    }

    private int getRetryCount(Message message) {
        var headers = message.getMessageProperties().getHeaders();
        var count = headers.get(X_RETRY_COUNT_HEADER);
        if (count instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }
}
