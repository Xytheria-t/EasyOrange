package com.cartethyia.easyorange.framework.messaging.nats;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.event.DomainEvent;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.api.PublishAck;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("NATS JetStream 双写 -> 测试")
class NatsEventDualWriterTest {

    @Mock
    private Connection connection;

    @Mock
    private JetStream jetStream;

    private record TestEvent(String eventId, String aggregateId) implements DomainEvent {}

    private NatsEventDualWriter writer() {
        NatsProperties properties = new NatsProperties();
        properties.setEnabled(true);
        return new NatsEventDualWriter(connection, properties, new ObjectMapper());
    }

    @Test
    @DisplayName("事务提交后 -> 双写 JetStream（主题 = 前缀 + 事件类型小写）")
    void dualWritesAfterCommit() throws Exception {
        when(connection.jetStream()).thenReturn(jetStream);
        PublishAck ack = mock(PublishAck.class);
        when(ack.getStream()).thenReturn("stream");
        when(jetStream.publish(anyString(), any())).thenReturn(ack);

        NatsEventDualWriter writer = writer();
        writer.onDomainEvent(new TestEvent("evt-1", "order-1"));

        verify(jetStream).publish(eq("eo.events.test"), any(byte[].class));
    }

    @Test
    @DisplayName("JetStream 不可用 -> 只告警不抛出（双写是副本，不影响主链路）")
    void dualWriteFailureSwallowed() throws Exception {
        when(connection.jetStream()).thenThrow(new RuntimeException("nats down"));

        NatsEventDualWriter writer = writer();
        writer.onDomainEvent(new TestEvent("evt-2", "order-1"));
    }

    @Test
    @DisplayName("JetStream publish 抛异常 -> 告警不抛出（双写副本失败不阻塞主链路）")
    void publishErrorSwallowed() throws Exception {
        when(connection.jetStream()).thenReturn(jetStream);
        when(jetStream.publish(anyString(), any())).thenThrow(new RuntimeException("no responders"));

        NatsEventDualWriter writer = writer();
        writer.onDomainEvent(new TestEvent("evt-3", "order-1"));
    }

    @Test
    @DisplayName("事件类型去 Event 后缀 -> 路由主题稳定")
    void subjectUsesEventType() throws Exception {
        when(connection.jetStream()).thenReturn(jetStream);
        PublishAck ack = mock(PublishAck.class);
        when(ack.getStream()).thenReturn("stream");
        when(jetStream.publish(anyString(), any())).thenReturn(ack);

        NatsProperties properties = new NatsProperties();
        properties.setSubjectPrefix("custom.prefix");
        NatsEventDualWriter writer = new NatsEventDualWriter(connection, properties, new ObjectMapper());
        writer.onDomainEvent(new TestEvent("evt-4", "order-1"));

        verify(jetStream).publish(eq("custom.prefix.test"), any(byte[].class));
        verify(connection, never()).close();
    }
}
