package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import tools.jackson.databind.json.JsonMapper;

/**
 * RabbitMQ 发布→消费全链路往返测试（真实 broker，复用 compose dev 栈），覆盖两条转换路径：
 * <ul>
 *   <li>具体类型监听器：载荷类型取自方法签名（{@code inferredArgumentType}），不经 {@code __TypeId__} 信任检查；</li>
 *   <li>纯 {@code Message} 监听器（与 {@code DlqAnomalyListener} 同形）：按 {@code __TypeId__} 反序列化，
 *       依赖转换器信任包配置，否则抛 "not in the trusted packages" 导致消息被丢弃。</li>
 * </ul>
 * 现有消费端单测直接调 handler 方法，绕过了真实 broker 与转换器装配，故需要往返覆盖。
 * 测试用独立队列/路由键，与生产拓扑隔离。
 */
class MessagingRoundTripIT extends AbstractIntegrationTest {

    private static final String TYPED_QUEUE = "eo.test.roundtrip";
    private static final String TYPED_ROUTING_KEY = "test.roundtrip";

    private static final String TYPEID_QUEUE = "eo.test.typeid";
    private static final String TYPEID_ROUTING_KEY = "test.typeid";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("领域事件经真实 broker 发布→消费往返，具体类型监听器反序列化为原事件")
    void typedListenerRoundTripsDomainEvent() throws InterruptedException {
        var event = new OrderCreatedEvent(
                "evt-roundtrip-1",
                "order-1",
                "buyer-1",
                "seller-1",
                List.of(new OrderCreatedEvent.OrderItemPayload(
                        "product-1", 1, new BigDecimal("99.99"), new BigDecimal("99.99"))),
                new BigDecimal("99.99"));

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, TYPED_ROUTING_KEY, event);

        assertThat(TypedListener.LATCH.await(10, TimeUnit.SECONDS))
                .as("事件应在超时前经 broker 往返到达监听器（消费端反序列化失败则超时）")
                .isTrue();
        assertThat(TypedListener.RECEIVED.get()).isEqualTo(event);
    }

    @Test
    @DisplayName("仅带 __TypeId__ 头的消息能被纯 Message 监听器接收（DLQ 异常监听场景）")
    void typeIdOnlyMessageReachesMessageOnlyListener() throws Exception {
        var event = new OrderCreatedEvent(
                "evt-typeid-1",
                "order-2",
                "buyer-2",
                "seller-2",
                List.of(new OrderCreatedEvent.OrderItemPayload(
                        "product-2", 1, new BigDecimal("59.90"), new BigDecimal("59.90"))),
                new BigDecimal("59.90"));

        // 绕过 template 转换器直发：只带 __TypeId__ 类型头，模拟死信消息的原始形态
        var props = new MessageProperties();
        props.setContentType("application/json");
        props.setHeader("__TypeId__", OrderCreatedEvent.class.getName());
        var body = JsonMapper.builder().build().writeValueAsBytes(event);
        rabbitTemplate.send(RabbitMQConfig.EXCHANGE_NAME, TYPEID_ROUTING_KEY, new Message(body, props));

        assertThat(TypeIdOnlyListener.LATCH.await(10, TimeUnit.SECONDS))
                .as("仅带 __TypeId__ 的消息应能到达纯 Message 监听器（信任包缺失则反序列化抛异常）")
                .isTrue();
        assertThat(TypeIdOnlyListener.RECEIVED.get().getMessageProperties().getHeaders())
                .containsEntry("__TypeId__", OrderCreatedEvent.class.getName());
    }

    @TestConfiguration
    static class TypedListener {

        static final AtomicReference<OrderCreatedEvent> RECEIVED = new AtomicReference<>();
        static final CountDownLatch LATCH = new CountDownLatch(1);

        @RabbitListener(
                bindings =
                        @QueueBinding(
                                value = @Queue(value = TYPED_QUEUE, durable = "true"),
                                exchange = @Exchange(value = RabbitMQConfig.EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
                                key = TYPED_ROUTING_KEY))
        public void onOrderCreated(OrderCreatedEvent event, Message message) {
            RECEIVED.set(event);
            LATCH.countDown();
        }
    }

    /** 纯 Message 参数监听器，与 {@code DlqAnomalyListener} 同形：无载荷类型可推断，走 __TypeId__ 路径。 */
    @TestConfiguration
    static class TypeIdOnlyListener {

        static final AtomicReference<Message> RECEIVED = new AtomicReference<>();
        static final CountDownLatch LATCH = new CountDownLatch(1);

        @RabbitListener(
                bindings =
                        @QueueBinding(
                                value = @Queue(value = TYPEID_QUEUE, durable = "true"),
                                exchange = @Exchange(value = RabbitMQConfig.EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
                                key = TYPEID_ROUTING_KEY))
        public void onRawMessage(Message message) {
            RECEIVED.set(message);
            LATCH.countDown();
        }
    }
}
