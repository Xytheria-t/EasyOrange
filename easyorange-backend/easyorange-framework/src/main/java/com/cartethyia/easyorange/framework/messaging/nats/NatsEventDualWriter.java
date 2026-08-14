package com.cartethyia.easyorange.framework.messaging.nats;

import com.cartethyia.easyorange.common.event.DomainEvent;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.api.PublishAck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

/**
 * NATS JetStream 消息双写 — 事务提交后把领域事件再写一份到 JetStream（best-effort）。
 * <p>
 * 定位（ADR-0005 演进位）：RabbitMQ 仍是主总线（Modulith Outbox 双写业务表 + EVENT_PUBLICATION
 * 同事务原子），NATS 是「第二写路径」——评估 NATS 替换/并存时的落点：
 * ① 事务提交后才发布（AFTER_COMMIT + fallbackExecution），不在事务内引入外部 IO；
 * ② 双写失败只告警不阻塞主链路（它是观测/演进用副本，不是一致性承诺）；
 * ③ 消息体与 RabbitMQ 路径一致（同一事件对象序列化，事件自带 eventId），消费侧若切换 NATS，
 * 把 {@code @RabbitListener} 消费者改为 JetStream 订阅 + 同一套 {@code EventIdempotencyChecker}
 * 幂等即可（幂等键 = eventType + 事件自身的 eventId，与投递路径无关）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "easyorange.messaging.nats.enabled", havingValue = "true")
@RequiredArgsConstructor
public class NatsEventDualWriter {

    private final Connection connection;
    private final NatsProperties properties;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDomainEvent(DomainEvent event) {
        try {
            JetStream jetStream = connection.jetStream();
            PublishAck ack = jetStream.publish(subject(event), objectMapper.writeValueAsBytes(event));
            log.debug(
                    "NATS dual-write published event={} subject={} stream={} seq={}",
                    event.eventType(),
                    subject(event),
                    ack.getStream(),
                    ack.getSeqno());
        } catch (Exception e) {
            // 双写是演进位副本：失败不影响 RabbitMQ 主链路（outbox 保证 at-least-once）
            log.warn("NATS dual-write failed (best-effort), event={}", event.eventType(), e);
        }
    }

    private String subject(DomainEvent event) {
        return properties.getSubjectPrefix() + "." + event.eventType().toLowerCase();
    }
}
