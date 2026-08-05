package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.WsMessage;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendMessageCommand;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketHandler 单元测试")
class ChatWebSocketHandlerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessageCommandHandler messageCommandHandler;

    @Mock
    private TypingIndicatorService typingService;

    @InjectMocks
    private ChatWebSocketHandler handler;

    @Captor
    private ArgumentCaptor<SendMessageCommand> commandCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    private static final String USER_ID = "1";
    private static final String RECEIVER_ID = "2";
    private static final String CONVERSATION_ID = "conv_1_2";

    private Principal principal;
    private WsMessage wsMessage;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        // lenient：broadcastRecallEvent / broadcastUnreadUpdate 用例不使用 principal
        lenient().when(principal.getName()).thenReturn(USER_ID);
        wsMessage = WsMessage.builder()
                .receiverId(RECEIVER_ID)
                .type(2)
                .title("你好")
                .content("hello")
                .conversationId(CONVERSATION_ID)
                .build();
    }

    @Nested
    @DisplayName("handleChatMessage")
    class HandleChatMessageTests {

        @Test
        @DisplayName("正常发送聊天消息")
        void handleChatMessage_normal_sendsMessage() {
            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                handler.handleChatMessage(wsMessage, principal);

                verify(messageCommandHandler).handle(commandCaptor.capture());

                SendMessageCommand cmd = commandCaptor.getValue();
                assertThat(cmd.receiverId()).isEqualTo(RECEIVER_ID);
                assertThat(cmd.type()).isEqualTo(2);
                assertThat(cmd.title()).isEqualTo("你好");
                assertThat(cmd.content()).isEqualTo("hello");
                assertThat(cmd.conversationId()).isEqualTo(CONVERSATION_ID);

                verify(messagingTemplate).convertAndSend(
                        eq("/queue/chat/" + CONVERSATION_ID),
                        eq(wsMessage)
                );
                verify(messagingTemplate).convertAndSendToUser(
                        eq(String.valueOf(RECEIVER_ID)),
                        eq("/queue/unread-count"),
                        any(Map.class)
                );
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("发送消息时 title 为 null 使用默认空字符串")
        void handleChatMessage_nullTitle_usesDefault() {
            wsMessage.setTitle(null);

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                handler.handleChatMessage(wsMessage, principal);

                verify(messageCommandHandler).handle(commandCaptor.capture());
                assertThat(commandCaptor.getValue().title()).isEmpty();
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("发送消息时 type 为 null 原样透传（归一化在命令处理器）")
        void handleChatMessage_nullType_passesThrough() {
            wsMessage.setType(null);

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                handler.handleChatMessage(wsMessage, principal);

                verify(messageCommandHandler).handle(commandCaptor.capture());
                assertThat(commandCaptor.getValue().type()).isNull();
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("发送消息时 businessId 传递正确")
        void handleChatMessage_withBusinessId_passesCorrectly() {
            wsMessage.setBusinessId("999");

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                handler.handleChatMessage(wsMessage, principal);

                verify(messageCommandHandler).handle(commandCaptor.capture());
                assertThat(commandCaptor.getValue().businessId()).isEqualTo("999");
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("发送消息时未读通知包含正确字段")
        void handleChatMessage_unreadNotification_containsCorrectFields() {
            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                handler.handleChatMessage(wsMessage, principal);

                verify(messagingTemplate).convertAndSendToUser(
                        eq(String.valueOf(RECEIVER_ID)),
                        eq("/queue/unread-count"),
                        mapCaptor.capture()
                );
                Map<String, Object> notification = mapCaptor.getValue();
                assertThat(notification)
                        .containsEntry("conversationId", CONVERSATION_ID)
                        .containsEntry("increment", 1)
                        .containsKey("timestamp");
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("handleDomainException")
    class HandleDomainExceptionTests {

        @Test
        @DisplayName("命令异常（限流）映射为发送方错误帧")
        void handleDomainException_sendsErrorFrame() {
            when(principal.getName()).thenReturn(USER_ID);
            MessageDomainException ex = new MessageDomainException("发送过于频繁，请稍后再试");

            handler.handleDomainException(ex, principal);

            verify(messagingTemplate).convertAndSendToUser(
                    eq(String.valueOf(USER_ID)),
                    eq("/queue/error"),
                    mapCaptor.capture()
            );
            assertThat(mapCaptor.getValue())
                    .containsEntry("type", "RATE_LIMITED")
                    .containsEntry("message", "发送过于频繁，请稍后再试");
        }
    }

    @Nested
    @DisplayName("handleTyping")
    class HandleTypingTests {

        @Test
        @DisplayName("正常发送正在输入指示")
        void handleTyping_normal_broadcasts() {
            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                handler.handleTyping(wsMessage, principal);

                verify(typingService).setTyping(CONVERSATION_ID, USER_ID);
                verify(messagingTemplate).convertAndSend(
                        eq("/topic/chat/" + CONVERSATION_ID + "/typing"),
                        (Object) mapCaptor.capture()
                );
                Map<String, Object> payload = mapCaptor.getValue();
                assertThat(payload)
                        .containsEntry("userId", USER_ID)
                        .containsKey("timestamp");
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("正在输入时 conversationId 为 null 仍尝试设置")
        void handleTyping_nullConversationId_stillSetsTyping() {
            wsMessage.setConversationId(null);

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                handler.handleTyping(wsMessage, principal);

                verify(typingService).setTyping(null, USER_ID);
                verify(messagingTemplate).convertAndSend(
                        eq("/topic/chat/null/typing"),
                        (Object) any(Map.class)
                );
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("正在输入时广播到正确主题")
        void handleTyping_broadcastsToCorrectTopic() {
            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                handler.handleTyping(wsMessage, principal);

                verify(messagingTemplate).convertAndSend(
                        eq("/topic/chat/" + CONVERSATION_ID + "/typing"),
                        (Object) any(Map.class)
                );
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("broadcastRecallEvent")
    class BroadcastRecallEventTests {

        @Test
        @DisplayName("广播撤回事件包含正确字段")
        void broadcastRecallEvent_containsCorrectFields() {
            String messageId = "100";

            handler.broadcastRecallEvent(CONVERSATION_ID, messageId, USER_ID);

            verify(messagingTemplate).convertAndSend(
                    eq("/topic/chat/" + CONVERSATION_ID + "/recall"),
                    (Object) mapCaptor.capture()
            );
            Map<String, Object> payload = mapCaptor.getValue();
            assertThat(payload)
                    .containsEntry("messageId", String.valueOf(messageId))
                    .containsEntry("conversationId", CONVERSATION_ID)
                    .containsEntry("operatorId", String.valueOf(USER_ID))
                    .containsKey("recalledAt");
            assertThat(payload.get("recalledAt")).isNotNull();
        }

        @Test
        @DisplayName("广播撤回事件到正确的主题")
        void broadcastRecallEvent_correctTopic() {
            handler.broadcastRecallEvent(CONVERSATION_ID, "100", USER_ID);

            verify(messagingTemplate).convertAndSend(
                    eq("/topic/chat/" + CONVERSATION_ID + "/recall"),
                    (Object) any(Map.class)
            );
        }
    }

    @Nested
    @DisplayName("broadcastUnreadUpdate")
    class BroadcastUnreadUpdateTests {

        @Test
        @DisplayName("广播未读更新包含正确字段")
        void broadcastUnreadUpdate_containsCorrectFields() {
            int count = 5;

            handler.broadcastUnreadUpdate(String.valueOf(RECEIVER_ID), CONVERSATION_ID, count);

            verify(messagingTemplate).convertAndSendToUser(
                    eq(String.valueOf(RECEIVER_ID)),
                    eq("/queue/unread-count"),
                    mapCaptor.capture()
            );
            Map<String, Object> payload = mapCaptor.getValue();
            assertThat(payload)
                    .containsEntry("conversationId", CONVERSATION_ID)
                    .containsEntry("count", count)
                    .containsKey("timestamp");
        }
    }
}
