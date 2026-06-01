package com.cartethyia.easyorange.framework.messaging;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@Tag("integration")
class RabbitMQDomainEventPublisherIT {

    @Container
    static GenericContainer<?> rabbitMQ = new GenericContainer<>(
        DockerImageName.parse("rabbitmq:3.13-management")
    )
        .withExposedPorts(5672, 15672)
        .waitingFor(org.testcontainers.containers.wait.strategy.Wait.forLogMessage(".*Server startup complete.*", 1));

    @DynamicPropertySource
    static void configureRabbitMQ(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbitMQ.getMappedPort(5672));
        registry.add("easyorange.rabbitmq.enabled", () -> "true");
    }

    @Autowired
    private DomainEventPublisher publisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void publish_orderCreatedEvent_messageArrivesInProductQueue() {
        var event = createTestEvent("OrderCreated");

        publisher.publish(event);

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                Message message = rabbitTemplate.receive("eo.product.events", 1000);
                assertThat(message).isNotNull();
            });
    }

    @Test
    void publish_multipleEvents_allArriveInQueues() {
        var event1 = createTestEvent("OrderCreated");
        var event2 = createTestEvent("OrderPaid");

        publisher.publish(event1);
        publisher.publish(event2);

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                List<Message> messages = new ArrayList<>();
                Message msg;
                while ((msg = rabbitTemplate.receive("eo.product.events", 100)) != null) {
                    messages.add(msg);
                }
                assertThat(messages).hasSizeGreaterThanOrEqualTo(2);
            });
    }

    private static BaseDomainEvent createTestEvent(String eventType) {
        return new BaseDomainEvent(RabbitMQDomainEventPublisherIT.class) {
            @Override
            public String eventType() {
                return eventType;
            }
        };
    }
}
