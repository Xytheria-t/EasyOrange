package com.cartethyia.easyorange.message.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate;
import com.cartethyia.easyorange.message.domain.event.MessageDeletedEvent;
import com.cartethyia.easyorange.message.domain.event.MessageReadEvent;
import com.cartethyia.easyorange.message.domain.event.MessageRecalledEvent;
import com.cartethyia.easyorange.message.domain.event.MessageSentEvent;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import com.cartethyia.easyorange.message.domain.exception.MessageNotFoundException;
import com.cartethyia.easyorange.message.domain.repository.MessageRepository;
import com.cartethyia.easyorange.message.domain.service.MessageRoutingService;
import com.cartethyia.easyorange.message.domain.service.OfflineMessageStoreService;
import com.cartethyia.easyorange.message.domain.service.RateLimiterService;
import com.cartethyia.easyorange.message.domain.service.SensitiveWordFilterService;
import com.cartethyia.easyorange.message.enums.MessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageCommandHandler 单元测试")
class MessageCommandHandlerTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private MessageRoutingService routingService;

    @Mock
    private OfflineMessageStoreService offlineMessageStoreService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private SensitiveWordFilterService sensitiveWordFilterService;

    @InjectMocks
    private MessageCommandHandler commandHandler;

    private static final Long USER_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final Long MESSAGE_ID = 100L;

    @BeforeEach
    void setUp() {
    }

    private MessageAggregate createTestMessage() {
        return MessageAggregate.fromRaw(
                MESSAGE_ID, USER_ID, RECEIVER_ID, 2, "标题", "hello",
                MessageStatus.UNREAD.getCode(), null, null,
                MessageStatus.SENT.getCode(), null, LocalDateTime.now());
    }

    private MessageAggregate createTestMessageForRecall() {
        return MessageAggregate.fromRaw(
                MESSAGE_ID, USER_ID, RECEIVER_ID, 2, "标题", "hello",
                MessageStatus.UNREAD.getCode(), null, null,
                MessageStatus.SENT.getCode(), null, LocalDateTime.now().minusMinutes(1));
    }

    @Nested
    @DisplayName("handle(SendMessageCommand)")
    class SendMessageTests {

        @Test
        @DisplayName("正常发送消息")
        void handle_sendMessage_success() {
            SendMessageCommand command = SendMessageCommand.builder()
                    .receiverId(RECEIVER_ID)
                    .type(2)
                    .title("标题")
                    .content("hello")
                    .build();

            when(rateLimiterService.allowSendMessage(anyLong())).thenReturn(true);
            when(sensitiveWordFilterService.filter(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
            MessageRoutingService.RouteDecision decision = new MessageRoutingService.RouteDecision(true, List.of());
            when(routingService.decideRoute(anyLong())).thenReturn(decision);

            MessageAggregate savedAggregate = MessageAggregate.fromRaw(
                    MESSAGE_ID, USER_ID, RECEIVER_ID, 2, "标题", "hello",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());
            when(messageRepository.save(any(MessageAggregate.class))).thenReturn(savedAggregate);

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository).save(any(MessageAggregate.class));
                verify(rateLimiterService).allowSendMessage(USER_ID);
                verify(sensitiveWordFilterService).filter("hello");
                verify(domainEventPublisher).publish(any(MessageSentEvent.class));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("发送过于频繁时抛出异常")
        void handle_sendMessage_rateLimited_throws() {
            SendMessageCommand command = SendMessageCommand.builder()
                    .receiverId(RECEIVER_ID)
                    .type(2)
                    .title("标题")
                    .content("hello")
                    .build();

            when(rateLimiterService.allowSendMessage(anyLong())).thenReturn(false);

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
            SendMessageCommand command = SendMessageCommand.builder()
                    .receiverId(RECEIVER_ID)
                    .type(2)
                    .title("标题")
                    .content("包含敏感词示例")
                    .build();

            when(rateLimiterService.allowSendMessage(anyLong())).thenReturn(true);
            when(sensitiveWordFilterService.filter("包含敏感词示例")).thenReturn("包含***");
            when(sensitiveWordFilterService.filter("标题")).thenReturn("标题");
            MessageRoutingService.RouteDecision decision = new MessageRoutingService.RouteDecision(true, List.of());
            when(routingService.decideRoute(anyLong())).thenReturn(decision);

            MessageAggregate savedAggregate = MessageAggregate.fromRaw(
                    MESSAGE_ID, USER_ID, RECEIVER_ID, 2, "标题", "包含***",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());
            when(messageRepository.save(any(MessageAggregate.class))).thenReturn(savedAggregate);

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository).save(argThat(msg ->
                        msg.content().equals("包含***")
                ));
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
            SendSystemMessageCommand command = SendSystemMessageCommand.builder()
                    .receiverId(RECEIVER_ID)
                    .title("系统通知")
                    .content("您的商品已审核通过")
                    .businessId(null)
                    .build();

            MessageRoutingService.RouteDecision decision = new MessageRoutingService.RouteDecision(true, List.of());
            when(routingService.decideRoute(anyLong())).thenReturn(decision);

            MessageAggregate savedAggregate = MessageAggregate.fromRaw(
                    MESSAGE_ID, null, RECEIVER_ID, 1, "系统通知", "您的商品已审核通过",
                    MessageStatus.UNREAD.getCode(), null, null,
                    null, null, LocalDateTime.now());
            when(messageRepository.save(any(MessageAggregate.class))).thenReturn(savedAggregate);

            commandHandler.handle(command);

            verify(messageRepository).save(any(MessageAggregate.class));
            verify(domainEventPublisher).publish(any(MessageSentEvent.class));
            verify(offlineMessageStoreService).storeIfOffline(anyLong(), any(), anyString(), eq(true));
        }
    }

    @Nested
    @DisplayName("handle(MarkAsReadCommand)")
    class MarkAsReadTests {

        @Test
        @DisplayName("正常标记已读")
        void handle_markAsRead_success() {
            MarkAsReadCommand command = MarkAsReadCommand.builder()
                    .messageId(MESSAGE_ID)
                    .build();

            MessageAggregate aggregate = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(RECEIVER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository).update(any(MessageAggregate.class));
                verify(domainEventPublisher).publish(any(MessageReadEvent.class));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("消息不存在时抛出异常")
        void handle_markAsRead_notFound_throws() {
            MarkAsReadCommand command = MarkAsReadCommand.builder()
                    .messageId(MESSAGE_ID)
                    .build();

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

            TestSecurityUtil.setSecurityContext(RECEIVER_ID);
            try {
                assertThatThrownBy(() -> commandHandler.handle(command))
                        .isInstanceOf(MessageNotFoundException.class);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("非接收者标记已读时抛出异常")
        void handle_markAsRead_notOwner_throws() {
            MarkAsReadCommand command = MarkAsReadCommand.builder()
                    .messageId(MESSAGE_ID)
                    .build();

            MessageAggregate aggregate = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(999L);
            try {
                assertThatThrownBy(() -> commandHandler.handle(command))
                        .isInstanceOf(BusinessException.class);
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
            MarkAsReadBatchCommand command = MarkAsReadBatchCommand.builder()
                    .messageIds(new ArrayList<>(List.of(MESSAGE_ID, 101L, 102L)))
                    .build();

            MessageAggregate msg1 = createTestMessage();
            MessageAggregate msg2 = MessageAggregate.fromRaw(
                    101L, USER_ID, RECEIVER_ID, 2, "标题", "hello",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());
            MessageAggregate msg3 = MessageAggregate.fromRaw(
                    102L, USER_ID, RECEIVER_ID, 2, "标题", "hello",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(msg1));
            when(messageRepository.findById(101L)).thenReturn(Optional.of(msg2));
            when(messageRepository.findById(102L)).thenReturn(Optional.of(msg3));

            TestSecurityUtil.setSecurityContext(RECEIVER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository, times(3)).update(any(MessageAggregate.class));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("批量标记时跳过不存在的消息")
        void handle_markAsReadBatch_skipNotFound() {
            MarkAsReadBatchCommand command = MarkAsReadBatchCommand.builder()
                    .messageIds(new ArrayList<>(List.of(MESSAGE_ID, 999L)))
                    .build();

            MessageAggregate msg1 = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(msg1));
            when(messageRepository.findById(999L)).thenReturn(Optional.empty());

            TestSecurityUtil.setSecurityContext(RECEIVER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository, times(1)).update(any(MessageAggregate.class));
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
            RecallMessageCommand command = RecallMessageCommand.builder()
                    .messageId(MESSAGE_ID)
                    .build();

            MessageAggregate aggregate = createTestMessageForRecall();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository).update(any(MessageAggregate.class));
                verify(domainEventPublisher).publish(any(MessageRecalledEvent.class));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("撤回不存在的消息抛出异常")
        void handle_recallMessage_notFound_throws() {
            RecallMessageCommand command = RecallMessageCommand.builder()
                    .messageId(MESSAGE_ID)
                    .build();

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                assertThatThrownBy(() -> commandHandler.handle(command))
                        .isInstanceOf(MessageNotFoundException.class);
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
            DeleteMessageCommand command = DeleteMessageCommand.builder()
                    .messageId(MESSAGE_ID)
                    .build();

            MessageAggregate aggregate = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(RECEIVER_ID);
            try {
                commandHandler.handle(command);

                verify(messageRepository).delete(MESSAGE_ID);
                verify(domainEventPublisher).publish(any(MessageDeletedEvent.class));
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("非接收者删除消息抛出异常")
        void handle_deleteMessage_notOwner_throws() {
            DeleteMessageCommand command = DeleteMessageCommand.builder()
                    .messageId(MESSAGE_ID)
                    .build();

            MessageAggregate aggregate = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(aggregate));

            TestSecurityUtil.setSecurityContext(999L);
            try {
                assertThatThrownBy(() -> commandHandler.handle(command))
                        .isInstanceOf(BusinessException.class);

                verify(messageRepository, never()).delete(anyLong());
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }
}
