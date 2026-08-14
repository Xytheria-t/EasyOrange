package com.cartethyia.easyorange.framework.messaging.nats;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * NATS JetStream 双写配置 — 事件驱动可靠性的「第二写路径」演进位（ADR-0005 备选 MQ）。
 * <p>
 * 默认关闭；开启后事务提交时把领域事件再写一份到 JetStream（不影响 RabbitMQ 主链路），
 * 消费侧迁移路径见 doc/agents/架构参考.md 的 NATS 章节。
 */
@Data
@ConfigurationProperties(prefix = "easyorange.messaging.nats")
public class NatsProperties {

    private boolean enabled = false;
    private String url = "nats://localhost:4222";
    /** JetStream 流名（不存在时启动自动创建）。 */
    private String stream = "eo-domain-events";
    /** 主题前缀，完整主题 = 前缀 + 事件类型小写（如 eo.events.ordercreated）。 */
    private String subjectPrefix = "eo.events";
}
