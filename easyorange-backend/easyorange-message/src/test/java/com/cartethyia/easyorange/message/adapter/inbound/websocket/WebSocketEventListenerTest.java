package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.message.application.service.OfflineMessageStoreService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketEventListener 单元测试")
class WebSocketEventListenerTest {

    @Mock
    private OfflineMessageStoreService offlineMessageStoreService;

    @InjectMocks
    private WebSocketEventListener eventListener;

    @Nested
    @DisplayName("连接/断开事件")
    class ConnectDisconnectTests {

        @Test
        @DisplayName("连接事件正常处理（无用户时仅记录日志）")
        void handleWebSocketConnectListener_processesEvent() {
            eventListener.handleWebSocketConnectListener(mock(SessionConnectedEvent.class));

            verify(offlineMessageStoreService, never()).replayPending(anyString());
        }

        @Test
        @DisplayName("连接事件携带用户时触发离线消息补推")
        void handleWebSocketConnectListener_replaysOfflineMessages() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            accessor.setSessionId("sess-1");
            accessor.setUser(new UsernamePasswordAuthenticationToken("user-1", null, List.of()));
            SessionConnectedEvent event = new SessionConnectedEvent(
                    this, MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

            eventListener.handleWebSocketConnectListener(event);

            verify(offlineMessageStoreService).replayPending("user-1");
        }

        @Test
        @DisplayName("断开事件正常处理")
        void handleWebSocketDisconnectListener_processesEvent() {
            eventListener.handleWebSocketDisconnectListener(mock(SessionDisconnectEvent.class));

            // 断开事件仅记录日志，验证不抛出异常
        }
    }
}
