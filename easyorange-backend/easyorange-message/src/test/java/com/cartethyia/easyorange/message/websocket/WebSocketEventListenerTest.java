package com.cartethyia.easyorange.message.websocket;

import com.cartethyia.easyorange.message.domain.event.MessageRecalledEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketEventListener 单元测试")
class WebSocketEventListenerTest {

    @Mock
    private ChatWebSocketHandler chatWebSocketHandler;

    @InjectMocks
    private WebSocketEventListener eventListener;

    @Nested
    @DisplayName("handleMessageRecalledEvent")
    class HandleMessageRecalledEventTests {

        @Test
        @DisplayName("接收到召回事件时调用 broadcastRecallEvent")
        void handleMessageRecalledEvent_callsBroadcastRecallEvent() {
            Long messageId = 100L;
            String conversationId = "conv_1_2";
            Long operatorId = 1L;
            LocalDateTime recalledAt = LocalDateTime.now();
            MessageRecalledEvent event = new MessageRecalledEvent(messageId, conversationId, operatorId, recalledAt);

            eventListener.handleMessageRecalledEvent(event);

            verify(chatWebSocketHandler).broadcastRecallEvent(conversationId, messageId, operatorId);
        }

        @Test
        @DisplayName("事件中的参数正确传递给 handler")
        void handleMessageRecalledEvent_passesCorrectArguments() {
            Long messageId = 200L;
            String conversationId = "conv_3_4";
            Long operatorId = 3L;
            LocalDateTime recalledAt = LocalDateTime.now().minusHours(1);
            MessageRecalledEvent event = new MessageRecalledEvent(messageId, conversationId, operatorId, recalledAt);

            eventListener.handleMessageRecalledEvent(event);

            verify(chatWebSocketHandler).broadcastRecallEvent(
                    eq("conv_3_4"),
                    eq(200L),
                    eq(3L)
            );
        }
    }

    @Nested
    @DisplayName("连接/断开事件")
    class ConnectDisconnectTests {

        @Test
        @DisplayName("连接事件正常处理")
        void handleWebSocketConnectListener_processesEvent() {
            eventListener.handleWebSocketConnectListener(mock(SessionConnectedEvent.class));

            // 连接事件仅记录日志，验证不抛出异常
        }

        @Test
        @DisplayName("断开事件正常处理")
        void handleWebSocketDisconnectListener_processesEvent() {
            eventListener.handleWebSocketDisconnectListener(mock(SessionDisconnectEvent.class));

            // 断开事件仅记录日志，验证不抛出异常
        }
    }
}
