package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.WsMessage;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendMessageCommand;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.enums.MessageStatus;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketHandler 单元测试")
class ChatWebSocketHandlerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessageCommandHandler messageCommandHandler;

    @InjectMocks
    private ChatWebSocketHandler handler;

    @Captor
    private ArgumentCaptor<SendMessageCommand> commandCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    /** 回显帧载荷：声明为 Object 才能选中 convertAndSend(String, Object) 而非 Map 重载。 */
    @Captor
    private ArgumentCaptor<Object> frameCaptor;

    private static final String USER_ID = "1";
    private static final String RECEIVER_ID = "2";
    private static final String CONVERSATION_ID = "conv_1_2";
    private static final LocalDateTime CREATE_TIME = LocalDateTime.of(2026, 3, 6, 10, 0);

    private Principal principal;
    private WsMessage wsMessage;

    /** 落库后的消息：回显帧的唯一来源（已过滤、带服务端 id 与 createTime）。 */
    private static Message savedMessage() {
        return Message.fromRaw(
                "100",
                USER_ID,
                RECEIVER_ID,
                MessageType.CHAT,
                "你好",
                "hello",
                ReadStatus.UNREAD,
                null,
                null,
                CONVERSATION_ID,
                MessageStatus.SENT,
                null,
                CREATE_TIME);
    }

    private void stubSavedMessage() {
        when(messageCommandHandler.sendMessage(eq(USER_ID), any())).thenReturn(savedMessage());
    }

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        // lenient：broadcastRecallEvent 用例不使用 principal
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
            stubSavedMessage();

            handler.handleChatMessage(wsMessage, principal);

            verify(messageCommandHandler).sendMessage(eq(USER_ID), commandCaptor.capture());

            SendMessageCommand cmd = commandCaptor.getValue();
            assertThat(cmd.receiverId()).isEqualTo(RECEIVER_ID);
            assertThat(cmd.type()).isEqualTo(2);
            assertThat(cmd.title()).isEqualTo("你好");
            assertThat(cmd.content()).isEqualTo("hello");

            verify(messagingTemplate)
                    .convertAndSendToUser(eq(String.valueOf(RECEIVER_ID)), eq("/queue/unread-count"), any(Map.class));
        }

        @Test
        @DisplayName("回显落库后的消息（服务端 id / 服务端 createTime），不是客户端原始帧")
        void handleChatMessage_broadcastsPersistedMessage() {
            stubSavedMessage();

            handler.handleChatMessage(wsMessage, principal);

            verify(messagingTemplate).convertAndSend(eq("/queue/chat/" + CONVERSATION_ID), frameCaptor.capture());
            @SuppressWarnings("unchecked")
            Map<String, Object> frame = (Map<String, Object>) frameCaptor.getValue();
            assertThat(frame)
                    .containsEntry("id", "100")
                    .containsEntry("senderId", USER_ID)
                    .containsEntry("receiverId", RECEIVER_ID)
                    .containsEntry("conversationId", CONVERSATION_ID)
                    .containsEntry("content", "hello")
                    .containsEntry("type", 2)
                    .containsEntry("status", "SENT")
                    .containsEntry("createTime", CREATE_TIME.toString());
        }

        @Test
        @DisplayName("会话 ID 取服务端算出的落库值，客户端没传也能定位房间")
        void handleChatMessage_usesServerConversationId() {
            wsMessage.setConversationId(null);
            stubSavedMessage();

            handler.handleChatMessage(wsMessage, principal);

            verify(messagingTemplate).convertAndSend(eq("/queue/chat/" + CONVERSATION_ID), (Object) any(Map.class));
        }

        @Test
        @DisplayName("发送消息时 title 为 null 使用默认空字符串")
        void handleChatMessage_nullTitle_usesDefault() {
            wsMessage.setTitle(null);
            stubSavedMessage();

            handler.handleChatMessage(wsMessage, principal);

            verify(messageCommandHandler).sendMessage(eq(USER_ID), commandCaptor.capture());
            assertThat(commandCaptor.getValue().title()).isEmpty();
        }

        @Test
        @DisplayName("发送消息时 type 为 null 原样透传（归一化在命令处理器）")
        void handleChatMessage_nullType_passesThrough() {
            wsMessage.setType(null);
            stubSavedMessage();

            handler.handleChatMessage(wsMessage, principal);

            verify(messageCommandHandler).sendMessage(eq(USER_ID), commandCaptor.capture());
            assertThat(commandCaptor.getValue().type()).isNull();
        }

        @Test
        @DisplayName("发送消息时 businessId 传递正确")
        void handleChatMessage_withBusinessId_passesCorrectly() {
            wsMessage.setBusinessId("999");
            stubSavedMessage();

            handler.handleChatMessage(wsMessage, principal);

            verify(messageCommandHandler).sendMessage(eq(USER_ID), commandCaptor.capture());
            assertThat(commandCaptor.getValue().businessId()).isEqualTo("999");
        }

        @Test
        @DisplayName("发送消息时未读通知包含正确字段")
        void handleChatMessage_unreadNotification_containsCorrectFields() {
            stubSavedMessage();

            handler.handleChatMessage(wsMessage, principal);

            verify(messagingTemplate)
                    .convertAndSendToUser(
                            eq(String.valueOf(RECEIVER_ID)), eq("/queue/unread-count"), mapCaptor.capture());
            Map<String, Object> notification = mapCaptor.getValue();
            assertThat(notification)
                    .containsEntry("conversationId", CONVERSATION_ID)
                    .containsEntry("increment", 1)
                    .containsKey("timestamp");
        }

        @Test
        @DisplayName("receiverId 缺失时入口即拒，不落库不广播")
        void handleChatMessage_missingReceiver_rejected() {
            wsMessage.setReceiverId(null);

            assertThatThrownBy(() -> handler.handleChatMessage(wsMessage, principal))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("接收方不能为空");

            verify(messageCommandHandler, never()).sendMessage(any(), any());
            verifyNoInteractions(messagingTemplate);
        }

        @Test
        @DisplayName("content 为空白时入口即拒")
        void handleChatMessage_blankContent_rejected() {
            wsMessage.setContent("  ");

            assertThatThrownBy(() -> handler.handleChatMessage(wsMessage, principal))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("消息内容不能为空");

            verify(messageCommandHandler, never()).sendMessage(any(), any());
        }
    }

    @Nested
    @DisplayName("错误帧映射")
    class ErrorFrameTests {

        @Test
        @DisplayName("命令异常（限流）映射为发送方错误帧")
        void handleDomainException_sendsErrorFrame() {
            MessageDomainException ex = MessageDomainException.of("发送过于频繁，请稍后再试");

            handler.handleDomainException(ex, principal);

            verify(messagingTemplate)
                    .convertAndSendToUser(eq(String.valueOf(USER_ID)), eq("/queue/error"), mapCaptor.capture());
            assertThat(mapCaptor.getValue())
                    .containsEntry("type", "RATE_LIMITED")
                    .containsEntry("message", "发送过于频繁，请稍后再试");
        }

        @Test
        @DisplayName("技术异常兜底成错误帧，且不把内部细节回显给发送方")
        void handleUnexpectedException_technicalError_sendsGenericFrame() {
            handler.handleUnexpectedException(new IllegalStateException("connection pool exhausted"), principal);

            verify(messagingTemplate)
                    .convertAndSendToUser(eq(String.valueOf(USER_ID)), eq("/queue/error"), mapCaptor.capture());
            assertThat(mapCaptor.getValue()).containsEntry("type", "ERROR").containsEntry("message", "消息处理失败，请稍后再试");
        }

        @Test
        @DisplayName("业务异常兜底沿用原校验文案（入参问题要能让发送方看见）")
        void handleUnexpectedException_businessError_keepsMessage() {
            handler.handleUnexpectedException(BusinessException.of("接收方不能为空"), principal);

            verify(messagingTemplate)
                    .convertAndSendToUser(eq(String.valueOf(USER_ID)), eq("/queue/error"), mapCaptor.capture());
            assertThat(mapCaptor.getValue()).containsEntry("type", "ERROR").containsEntry("message", "接收方不能为空");
        }

        @Test
        @DisplayName("无 Principal 时不投递错误帧（无处可投）")
        void handleUnexpectedException_noPrincipal_noFrame() {
            handler.handleUnexpectedException(new IllegalStateException("boom"), null);

            verifyNoInteractions(messagingTemplate);
        }
    }

    @Nested
    @DisplayName("handleTyping")
    class HandleTypingTests {

        @Test
        @DisplayName("正常广播正在输入指示")
        void handleTyping_normal_broadcasts() {
            handler.handleTyping(wsMessage, principal);

            verify(messagingTemplate)
                    .convertAndSend(eq("/topic/chat/" + CONVERSATION_ID + "/typing"), (Object) mapCaptor.capture());
            Map<String, Object> payload = mapCaptor.getValue();
            assertThat(payload).containsEntry("userId", USER_ID).containsKey("timestamp");
        }

        @Test
        @DisplayName("正在输入时 conversationId 为 null 仍广播（原样透传客户端话题）")
        void handleTyping_nullConversationId_stillBroadcasts() {
            wsMessage.setConversationId(null);

            handler.handleTyping(wsMessage, principal);

            verify(messagingTemplate).convertAndSend(eq("/topic/chat/null/typing"), (Object) any(Map.class));
        }

        @Test
        @DisplayName("正在输入时广播到正确主题")
        void handleTyping_broadcastsToCorrectTopic() {
            handler.handleTyping(wsMessage, principal);

            verify(messagingTemplate)
                    .convertAndSend(eq("/topic/chat/" + CONVERSATION_ID + "/typing"), (Object) any(Map.class));
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

            verify(messagingTemplate)
                    .convertAndSend(eq("/topic/chat/" + CONVERSATION_ID + "/recall"), (Object) mapCaptor.capture());
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

            verify(messagingTemplate)
                    .convertAndSend(eq("/topic/chat/" + CONVERSATION_ID + "/recall"), (Object) any(Map.class));
        }
    }
}
