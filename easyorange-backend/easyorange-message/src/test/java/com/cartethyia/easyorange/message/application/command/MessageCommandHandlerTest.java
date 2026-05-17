package com.cartethyia.easyorange.message.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
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
import com.cartethyia.easyorange.message.entity.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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

    private Message createTestMessage() {
        Message msg = Message.create(USER_ID, RECEIVER_ID, 2, "标题", "hello", null);
        msg.setId(MESSAGE_ID);
        msg.setCreateTime(LocalDateTime.now());
        return msg;
    }

    private Message createTestMessageForRecall() {
        Message msg = Message.create(USER_ID, RECEIVER_ID, 2, "标题", "hello", null);
        msg.setId(MESSAGE_ID);
        msg.setCreateTime(LocalDateTime.now().minusMinutes(1));
        return msg;
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

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(USER_ID);

                commandHandler.handle(command);

                verify(messageRepository).save(any(Message.class));
                verify(rateLimiterService).allowSendMessage(USER_ID);
                verify(sensitiveWordFilterService).filter("hello");
                verify(domainEventPublisher).publish(any(MessageSentEvent.class));
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

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(USER_ID);

                assertThatThrownBy(() -> commandHandler.handle(command))
                        .isInstanceOf(MessageDomainException.class)
                        .hasMessageContaining("发送过于频繁");

                verify(messageRepository, never()).save(any());
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

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(USER_ID);

                commandHandler.handle(command);

                verify(messageRepository).save(argThat(msg ->
                        msg.getContent().equals("包含***")
                ));
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

            commandHandler.handle(command);

            verify(messageRepository).save(any(Message.class));
            verify(domainEventPublisher).publish(any(MessageSentEvent.class));
            // message.getId() may be null before persistence; just verify interaction occurred
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

            Message message = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(RECEIVER_ID);

                commandHandler.handle(command);

                verify(messageRepository).update(message);
                verify(domainEventPublisher).publish(any(MessageReadEvent.class));
            }
        }

        @Test
        @DisplayName("消息不存在时抛出异常")
        void handle_markAsRead_notFound_throws() {
            MarkAsReadCommand command = MarkAsReadCommand.builder()
                    .messageId(MESSAGE_ID)
                    .build();

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(RECEIVER_ID);

                assertThatThrownBy(() -> commandHandler.handle(command))
                        .isInstanceOf(MessageNotFoundException.class);
            }
        }

        @Test
        @DisplayName("非接收者标记已读时抛出异常")
        void handle_markAsRead_notOwner_throws() {
            MarkAsReadCommand command = MarkAsReadCommand.builder()
                    .messageId(MESSAGE_ID)
                    .build();

            Message message = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(999L);

                assertThatThrownBy(() -> commandHandler.handle(command))
                        .isInstanceOf(BusinessException.class);
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

            Message msg1 = createTestMessage();
            Message msg2 = createTestMessage();
            msg2.setId(101L);
            Message msg3 = createTestMessage();
            msg3.setId(102L);

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(msg1));
            when(messageRepository.findById(101L)).thenReturn(Optional.of(msg2));
            when(messageRepository.findById(102L)).thenReturn(Optional.of(msg3));

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(RECEIVER_ID);

                commandHandler.handle(command);

                verify(messageRepository, times(3)).update(any(Message.class));
            }
        }

        @Test
        @DisplayName("批量标记时跳过不存在的消息")
        void handle_markAsReadBatch_skipNotFound() {
            MarkAsReadBatchCommand command = MarkAsReadBatchCommand.builder()
                    .messageIds(new ArrayList<>(List.of(MESSAGE_ID, 999L)))
                    .build();

            Message msg1 = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(msg1));
            when(messageRepository.findById(999L)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(RECEIVER_ID);

                commandHandler.handle(command);

                verify(messageRepository, times(1)).update(any(Message.class));
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

            Message message = createTestMessageForRecall();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(USER_ID);

                commandHandler.handle(command);

                verify(messageRepository).update(message);
                verify(domainEventPublisher).publish(any(MessageRecalledEvent.class));
            }
        }

        @Test
        @DisplayName("撤回不存在的消息抛出异常")
        void handle_recallMessage_notFound_throws() {
            RecallMessageCommand command = RecallMessageCommand.builder()
                    .messageId(MESSAGE_ID)
                    .build();

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(USER_ID);

                assertThatThrownBy(() -> commandHandler.handle(command))
                        .isInstanceOf(MessageNotFoundException.class);
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

            Message message = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(RECEIVER_ID);

                commandHandler.handle(command);

                verify(messageRepository).delete(MESSAGE_ID);
                verify(domainEventPublisher).publish(any(MessageDeletedEvent.class));
            }
        }

        @Test
        @DisplayName("非接收者删除消息抛出异常")
        void handle_deleteMessage_notOwner_throws() {
            DeleteMessageCommand command = DeleteMessageCommand.builder()
                    .messageId(MESSAGE_ID)
                    .build();

            Message message = createTestMessage();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));

            try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
                mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(999L);

                assertThatThrownBy(() -> commandHandler.handle(command))
                        .isInstanceOf(BusinessException.class);

                verify(messageRepository, never()).delete(anyLong());
            }
        }
    }
}
