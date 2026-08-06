package com.cartethyia.easyorange.message.application.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.message.application.service.OfflineMessageStoreService;
import com.cartethyia.easyorange.message.application.service.RateLimiterService;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.enums.MessageStatus;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.event.MessageRecalledEvent;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import com.cartethyia.easyorange.message.domain.exception.MessageNotFoundException;
import com.cartethyia.easyorange.message.domain.port.MessageNotifierPort;
import com.cartethyia.easyorange.message.domain.repository.MessageRepository;
import com.cartethyia.easyorange.message.domain.service.SensitiveWordFilterService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageCommandHandler 单元测试")
class MessageCommandHandlerTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private OfflineMessageStoreService offlineMessageStoreService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private SensitiveWordFilterService sensitiveWordFilterService;

    @Mock
    private MessageNotifierPort messageNotifier;

    @InjectMocks
    private MessageCommandHandler commandHandler;

    private static final String USER_ID = "1";
    private static final String RECEIVER_ID = "2";
    private static final String MESSAGE_ID = "100";

    @BeforeEach
    void setUp() {}

    private Message createTestMessage() {
        return Message.fromRaw(
                MESSAGE_ID,
                USER_ID,
                RECEIVER_ID,
                2,
                "标题",
                "hello",
                ReadStatus.UNREAD,
                null,
                null,
                MessageStatus.SENT,
                null,
                LocalDateTime.now());
    }

    private Message createTestMessageForRecall() {
        return Message.fromRaw(
                MESSAGE_ID,
                USER_ID,
                RECEIVER_ID,
                2,
                "标题",
                "hello",
                ReadStatus.UNREAD,
                null,
                null,
                MessageStatus.SENT,
                null,
                LocalDateTime.now().minusMinutes(1));
    }

    @Nested
    @DisplayName("handle(SendMessageCommand)")
    class SendMessageTests {

        @Test
        @DisplayName("正常发送消息")
        void handle_sendMessage_success() {
            SendMessageCommand command = new SendMessageCommand(RECEIVER_ID, 2, "标题", "hello", null, null);

            when(rateLimiterService.allowSendMessage(anyString())).thenReturn(true);
            when(sensitiveWordFilterService.filter(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
            when(messageNotifier.isUserOnline(anyString())).thenReturn(true);

            Message savedAggregate = Message.fromRaw(
                    MESSAGE_ID,
                    USER_ID,
                    RECEIVER_ID,
                    2,
                    "标题",
                    "hello",
                    ReadStatus.UNREAD,
                    null,
                    null,
                    MessageStatus.SENT,
                    null,
                    LocalDateTime.now());
            when(messageRepository.save(any(Message.class))).thenReturn(savedAggregate);

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository).save(any(Message.class));
                verify(rateLimiterService).allowSendMessage(USER_ID);
                verify(sensitiveWordFilterService).filter("hello");
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("发送过于频繁时抛出异常")
        void handle_sendMessage_rateLimited_throws() {
            SendMessageCommand command = new SendMessageCommand(RECEIVER_ID, 2, "标题", "hello", null, null);

            when(rateLimiterService.allowSendMessage(anyString())).thenReturn(false);

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                assertThatThrownBy(() -> commandHandler.handle(command))
                        .isInstanceOf(MessageDomainException.class)
                        .hasMessageContaining("发送过于频繁");

                verify(messageRepository, never()).save(any());
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("发送消息经过敏感词过滤")
        void handle_sendMessage_sensitiveFilterApplied() {
            SendMessageCommand command = new SendMessageCommand(RECEIVER_ID, 2, "标题", "包含敏感词示例", null, null);

            when(rateLimiterService.allowSendMessage(anyString())).thenReturn(true);
            when(sensitiveWordFilterService.filter("包含敏感词示例")).thenReturn("包含***");
            when(sensitiveWordFilterService.filter("标题")).thenReturn("标题");
            when(messageNotifier.isUserOnline(anyString())).thenReturn(true);

            Message savedAggregate = Message.fromRaw(
                    MESSAGE_ID,
                    USER_ID,
                    RECEIVER_ID,
                    2,
                    "标题",
                    "包含***",
                    ReadStatus.UNREAD,
                    null,
                    null,
                    MessageStatus.SENT,
                    null,
                    LocalDateTime.now());
            when(messageRepository.save(any(Message.class))).thenReturn(savedAggregate);

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository).save(argThat(msg -> msg.content().equals("包含***")));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("type 缺省时归一化为聊天消息（CHAT=2）")
        void handle_sendMessage_nullType_defaultsToChat() {
            SendMessageCommand command = new SendMessageCommand(RECEIVER_ID, null, "标题", "hello", null, null);

            when(rateLimiterService.allowSendMessage(anyString())).thenReturn(true);
            when(sensitiveWordFilterService.filter(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
            when(messageNotifier.isUserOnline(anyString())).thenReturn(true);

            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository).save(argThat(msg -> msg.type() == 2));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("handle(SendSystemMessageCommand)")
    class SendSystemMessageTests {

        @Test
        @DisplayName("正常发送系统消息")
        void handle_sendSystemMessage_success() {
            SendSystemMessageCommand command = new SendSystemMessageCommand(RECEIVER_ID, "系统通知", "您的商品已审核通过", null);

            when(messageNotifier.isUserOnline(anyString())).thenReturn(true);

            Message savedAggregate = Message.fromRaw(
                    MESSAGE_ID,
                    null,
                    RECEIVER_ID,
                    1,
                    "系统通知",
                    "您的商品已审核通过",
                    ReadStatus.UNREAD,
                    null,
                    null,
                    null,
                    null,
                    LocalDateTime.now());
            when(messageRepository.save(any(Message.class))).thenReturn(savedAggregate);

            commandHandler.handle(command);

            verify(messageRepository).save(any(Message.class));
            verify(offlineMessageStoreService).storeIfOffline(anyString(), any(), anyString(), eq(true));
            verify(messageNotifier).sendNotification(eq(RECEIVER_ID), any());
        }
    }

    @Nested
    @DisplayName("handle(MarkAsReadCommand)")
    class MarkAsReadTests {

        @Test
        @DisplayName("正常标记已读")
        void handle_markAsRead_success() {
            MarkAsReadCommand command = new MarkAsReadCommand(MESSAGE_ID);

            Message aggregate = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(RECEIVER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository).update(any(Message.class));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("消息不存在时抛出异常")
        void handle_markAsRead_notFound_throws() {
            MarkAsReadCommand command = new MarkAsReadCommand(MESSAGE_ID);

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

            TestSecurityUtil.setSecurityContext(RECEIVER_ID);
            try {
                assertThatThrownBy(() -> commandHandler.handle(command)).isInstanceOf(MessageNotFoundException.class);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("非接收者标记已读时抛出异常")
        void handle_markAsRead_notOwner_throws() {
            MarkAsReadCommand command = new MarkAsReadCommand(MESSAGE_ID);

            Message aggregate = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext("999");
            try {
                assertThatThrownBy(() -> commandHandler.handle(command)).isInstanceOf(BusinessException.class);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("handle(MarkAsReadBatchCommand)")
    class MarkAsReadBatchTests {

        @Test
        @DisplayName("批量标记已读成功")
        void handle_markAsReadBatch_success() {
            MarkAsReadBatchCommand command =
                    new MarkAsReadBatchCommand(new ArrayList<>(List.of(MESSAGE_ID, "101", "102")));

            Message msg1 = createTestMessage();
            Message msg2 = Message.fromRaw(
                    "101",
                    USER_ID,
                    RECEIVER_ID,
                    2,
                    "标题",
                    "hello",
                    ReadStatus.UNREAD,
                    null,
                    null,
                    MessageStatus.SENT,
                    null,
                    LocalDateTime.now());
            Message msg3 = Message.fromRaw(
                    "102",
                    USER_ID,
                    RECEIVER_ID,
                    2,
                    "标题",
                    "hello",
                    ReadStatus.UNREAD,
                    null,
                    null,
                    MessageStatus.SENT,
                    null,
                    LocalDateTime.now());

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(msg1));
            when(messageRepository.findById("101")).thenReturn(Optional.of(msg2));
            when(messageRepository.findById("102")).thenReturn(Optional.of(msg3));

            TestSecurityUtil.setSecurityContext(RECEIVER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository, times(3)).update(any(Message.class));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("批量标记时跳过不存在的消息")
        void handle_markAsReadBatch_skipNotFound() {
            MarkAsReadBatchCommand command = new MarkAsReadBatchCommand(new ArrayList<>(List.of(MESSAGE_ID, "999")));

            Message msg1 = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(msg1));
            when(messageRepository.findById("999")).thenReturn(Optional.empty());

            TestSecurityUtil.setSecurityContext(RECEIVER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository, times(1)).update(any(Message.class));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("handle(RecallMessageCommand)")
    class RecallMessageTests {

        @Test
        @DisplayName("正常撤回消息")
        void handle_recallMessage_success() {
            RecallMessageCommand command = new RecallMessageCommand(MESSAGE_ID);

            Message aggregate = createTestMessageForRecall();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository).update(any(Message.class));
                verify(domainEventPublisher).publish(any(MessageRecalledEvent.class));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("撤回不存在的消息抛出异常")
        void handle_recallMessage_notFound_throws() {
            RecallMessageCommand command = new RecallMessageCommand(MESSAGE_ID);

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                assertThatThrownBy(() -> commandHandler.handle(command)).isInstanceOf(MessageNotFoundException.class);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("非发送者撤回时抛出异常")
        void handle_recallMessage_notSender_throws() {
            RecallMessageCommand command = new RecallMessageCommand(MESSAGE_ID);

            Message aggregate = createTestMessageForRecall();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext("999");
            try {
                assertThatThrownBy(() -> commandHandler.handle(command)).isInstanceOf(BusinessException.class);

                verify(messageRepository, never()).update(any());
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("handle(DeleteMessageCommand)")
    class DeleteMessageTests {

        @Test
        @DisplayName("正常删除消息")
        void handle_deleteMessage_success() {
            DeleteMessageCommand command = new DeleteMessageCommand(MESSAGE_ID);

            Message aggregate = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(RECEIVER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository).delete(MESSAGE_ID);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("非接收者删除消息抛出异常")
        void handle_deleteMessage_notOwner_throws() {
            DeleteMessageCommand command = new DeleteMessageCommand(MESSAGE_ID);

            Message aggregate = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext("999");
            try {
                assertThatThrownBy(() -> commandHandler.handle(command)).isInstanceOf(BusinessException.class);

                verify(messageRepository, never()).delete(anyString());
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }
}
