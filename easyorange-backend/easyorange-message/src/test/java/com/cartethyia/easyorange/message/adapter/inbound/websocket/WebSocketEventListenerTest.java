package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketEventListener 单元测试")
class WebSocketEventListenerTest {

    @InjectMocks
    private WebSocketEventListener eventListener;

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
