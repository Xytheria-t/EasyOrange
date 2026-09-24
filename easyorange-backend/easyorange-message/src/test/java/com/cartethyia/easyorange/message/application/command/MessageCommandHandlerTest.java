package com.cartethyia.easyorange.message.application.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.util.DistributedRateLimiter;
import com.cartethyia.easyorange.message.application.service.OfflineMessageStoreService;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.enums.MessageStatus;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.event.MessageRecalledEvent;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import com.cartethyia.easyorange.message.domain.port.MessageNotifierPort;
import com.cartethyia.easyorange.message.domain.repository.MessageRepository;
import com.cartethyia.easyorange.message.domain.service.SensitiveWordFilterService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private DistributedRateLimiter distributedRateLimiter;

    @Mock
    private SensitiveWordFilterService sensitiveWordFilterService;

    @Mock
    private MessageNotifierPort messageNotifier;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private MessageCommandHandler commandHandler;

    private static final String GENERATED_ID = "gen-msg-1";
    private static final String USER_ID = "1";
    private static final String RECEIVER_ID = "2";
    private static final String MESSAGE_ID = "100";

    private Message createTestMessage() {
        return Message.fromRaw(
                MESSAGE_ID,
                USER_ID,
                RECEIVER_ID,
                MessageType.CHAT,
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
                MessageType.CHAT,
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
    @DisplayName("sendMessage(SendMessageCommand)")
    class SendMessageTests {

        @Test
        @DisplayName("正常发送消息")
        void sendMessage_success() {
            SendMessageCommand command = new SendMessageCommand(RECEIVER_ID, 2, "标题", "hello", null, null);

            when(distributedRateLimiter.tryAcquire(anyString(), anyLong(), anyLong()))
                    .thenReturn(true);
            when(sensitiveWordFilterService.filter(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
            when(messageNotifier.isUserOnline(anyString())).thenReturn(true);
            when(idGenerator.generateId()).thenReturn(GENERATED_ID);

            Message savedAggregate = Message.fromRaw(
                    MESSAGE_ID,
                    USER_ID,
                    RECEIVER_ID,
                    MessageType.CHAT,
                    "标题",
                    "hello",
                    ReadStatus.UNREAD,
                    null,
                    null,
                    MessageStatus.SENT,
                    null,
                    LocalDateTime.now());
            when(messageRepository.save(any(Message.class))).thenReturn(savedAggregate);

            commandHandler.sendMessage(USER_ID, command);

            verify(messageRepository).save(argThat(msg -> GENERATED_ID.equals(msg.id())));
            verify(distributedRateLimiter).tryAcquire(eq("eo:rate:message:" + USER_ID), anyLong(), anyLong());
            verify(sensitiveWordFilterService).filter("hello");
        }

        @Test
        @DisplayName("发送过于频繁时抛出异常")
        void sendMessage_rateLimited_throws() {
            SendMessageCommand command = new SendMessageCommand(RECEIVER_ID, 2, "标题", "hello", null, null);

            when(distributedRateLimiter.tryAcquire(anyString(), anyLong(), anyLong()))
                    .thenReturn(false);

            assertThatThrownBy(() -> commandHandler.sendMessage(USER_ID, command))
                    .isInstanceOf(MessageDomainException.class)
                    .hasMessageContaining("发送过于频繁");

            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("发送消息经过敏感词过滤")
        void sendMessage_sensitiveFilterApplied() {
            SendMessageCommand command = new SendMessageCommand(RECEIVER_ID, 2, "标题", "包含敏感词示例", null, null);

            when(distributedRateLimiter.tryAcquire(anyString(), anyLong(), anyLong()))
                    .thenReturn(true);
            when(sensitiveWordFilterService.filter("包含敏感词示例")).thenReturn("包含***");
            when(sensitiveWordFilterService.filter("标题")).thenReturn("标题");
            when(messageNotifier.isUserOnline(anyString())).thenReturn(true);
            when(idGenerator.generateId()).thenReturn(GENERATED_ID);

            Message savedAggregate = Message.fromRaw(
                    MESSAGE_ID,
                    USER_ID,
                    RECEIVER_ID,
                    MessageType.CHAT,
                    "标题",
                    "包含***",
                    ReadStatus.UNREAD,
                    null,
                    null,
                    MessageStatus.SENT,
                    null,
                    LocalDateTime.now());
            when(messageRepository.save(any(Message.class))).thenReturn(savedAggregate);

            commandHandler.sendMessage(USER_ID, command);

            verify(messageRepository).save(argThat(msg -> msg.content().equals("包含***")));
        }

        @Test
        @DisplayName("type 缺省时归一化为聊天消息（CHAT=2）")
        void sendMessage_nullType_defaultsToChat() {
            SendMessageCommand command = new SendMessageCommand(RECEIVER_ID, null, "标题", "hello", null, null);

            when(distributedRateLimiter.tryAcquire(anyString(), anyLong(), anyLong()))
                    .thenReturn(true);
            when(sensitiveWordFilterService.filter(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
            when(messageNotifier.isUserOnline(anyString())).thenReturn(true);
            when(idGenerator.generateId()).thenReturn(GENERATED_ID);

            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

            commandHandler.sendMessage(USER_ID, command);

            verify(messageRepository).save(argThat(msg -> msg.type() == MessageType.CHAT));
        }
    }

    @Nested
    @DisplayName("sendSystemMessage(SendSystemMessageCommand)")
    class SendSystemMessageTests {

        @Test
        @DisplayName("正常发送系统消息")
        void sendSystemMessage_success() {
            SendSystemMessageCommand command = new SendSystemMessageCommand(RECEIVER_ID, "系统通知", "您的商品已审核通过", null);

            when(messageNotifier.isUserOnline(anyString())).thenReturn(true);
            when(idGenerator.generateId()).thenReturn(GENERATED_ID);

            Message savedAggregate = Message.fromRaw(
                    MESSAGE_ID,
                    null,
                    RECEIVER_ID,
                    MessageType.SYSTEM,
                    "系统通知",
                    "您的商品已审核通过",
                    ReadStatus.UNREAD,
                    null,
                    null,
                    null,
                    null,
                    LocalDateTime.now());
            when(messageRepository.save(any(Message.class))).thenReturn(savedAggregate);

            commandHandler.sendSystemMessage(command);

            verify(messageRepository).save(argThat(msg -> GENERATED_ID.equals(msg.id())));
            verify(offlineMessageStoreService).storeIfOffline(anyString(), any(), anyString(), eq(true));
            verify(messageNotifier).sendNotification(eq(RECEIVER_ID), any());
        }
    }

    @Nested
    @DisplayName("markAsRead(MarkAsReadCommand)")
    class MarkAsReadTests {

        @Test
        @DisplayName("正常标记已读")
        void markAsRead_success() {
            MarkAsReadCommand command = new MarkAsReadCommand(MESSAGE_ID);

            Message aggregate = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            commandHandler.markAsRead(RECEIVER_ID, command);

            verify(messageRepository).update(any(Message.class));
        }

        @Test
        @DisplayName("消息不存在时抛出异常")
        void markAsRead_notFound_throws() {
            MarkAsReadCommand command = new MarkAsReadCommand(MESSAGE_ID);

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commandHandler.markAsRead(RECEIVER_ID, command))
                    .isInstanceOf(MessageDomainException.class);
        }

        @Test
        @DisplayName("非接收者标记已读时抛出异常")
        void markAsRead_notOwner_throws() {
            MarkAsReadCommand command = new MarkAsReadCommand(MESSAGE_ID);

            Message aggregate = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            assertThatThrownBy(() -> commandHandler.markAsRead("999", command)).isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("markAsReadBatch(MarkAsReadBatchCommand)")
    class MarkAsReadBatchTests {

        @Test
        @DisplayName("批量标记已读：谓词下推为一次 markAsReadByIds，不再逐条读+写")
        void markAsReadBatch_success() {
            MarkAsReadBatchCommand command =
                    new MarkAsReadBatchCommand(new ArrayList<>(List.of(MESSAGE_ID, "101", "102")));

            commandHandler.markAsReadBatch(RECEIVER_ID, command);

            // 关键回归点：N 条消息只发一次批量更新，而非 N 次 findById + N 次 update
            verify(messageRepository).markAsReadByIds(RECEIVER_ID, List.of(MESSAGE_ID, "101", "102"));
            verify(messageRepository, never()).update(any(Message.class));
        }

        @Test
        @DisplayName("批量标记：不存在 / 非本人的 ID 由 SQL 谓词跳过，整批仍只发一次")
        void markAsReadBatch_skipNotFound() {
            MarkAsReadBatchCommand command = new MarkAsReadBatchCommand(new ArrayList<>(List.of(MESSAGE_ID, "999")));

            commandHandler.markAsReadBatch(RECEIVER_ID, command);

            verify(messageRepository).markAsReadByIds(RECEIVER_ID, List.of(MESSAGE_ID, "999"));
        }

        @Test
        @DisplayName("空列表 no-op 成功：无可标记不再回 B0002（TD-026）")
        void markAsReadBatch_emptyList_noop() {
            commandHandler.markAsReadBatch(RECEIVER_ID, new MarkAsReadBatchCommand(List.of()));

            verifyNoInteractions(messageRepository);
        }
    }

    @Nested
    @DisplayName("recallMessage(RecallMessageCommand)")
    class RecallMessageTests {

        @Test
        @DisplayName("正常撤回消息")
        void recallMessage_success() {
            RecallMessageCommand command = new RecallMessageCommand(MESSAGE_ID);

            Message aggregate = createTestMessageForRecall();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            commandHandler.recallMessage(USER_ID, command);

            verify(messageRepository).update(any(Message.class));
            verify(domainEventPublisher).publish(any(MessageRecalledEvent.class));
        }

        @Test
        @DisplayName("撤回不存在的消息抛出异常")
        void recallMessage_notFound_throws() {
            RecallMessageCommand command = new RecallMessageCommand(MESSAGE_ID);

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commandHandler.recallMessage(USER_ID, command))
                    .isInstanceOf(MessageDomainException.class);
        }

        @Test
        @DisplayName("非发送者撤回时抛出异常")
        void recallMessage_notSender_throws() {
            RecallMessageCommand command = new RecallMessageCommand(MESSAGE_ID);

            Message aggregate = createTestMessageForRecall();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            assertThatThrownBy(() -> commandHandler.recallMessage("999", command))
                    .isInstanceOf(BusinessException.class);

            verify(messageRepository, never()).update(any());
        }
    }
}
