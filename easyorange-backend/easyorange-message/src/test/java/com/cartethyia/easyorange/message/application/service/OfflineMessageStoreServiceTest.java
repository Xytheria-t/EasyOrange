package com.cartethyia.easyorange.message.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.message.application.port.query.MessageQueryRepository;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;
import com.cartethyia.easyorange.message.domain.constant.MessageConstant;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import com.cartethyia.easyorange.message.domain.enums.PushStatus;
import com.cartethyia.easyorange.message.domain.port.MessageNotifierPort;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OfflineMessageStoreService 单元测试")
class OfflineMessageStoreServiceTest {

    @Mock
    private OfflineMessageRepository offlineMessageRepository;

    @Mock
    private MessageQueryRepository messageQueryRepository;

    @Mock
    private MessageNotifierPort messageNotifier;

    @InjectMocks
    private OfflineMessageStoreService offlineMessageStoreService;

    private static final String USER_ID = "1";
    private static final String MESSAGE_ID = "100";
    private static final String PUSH_CHANNEL = "WEBSOCKET";

    @Nested
    @DisplayName("storeIfOffline")
    class StoreIfOfflineTests {

        @Test
        @DisplayName("用户离线时存储离线消息")
        void storeIfOffline_userOffline_savesMessage() {
            offlineMessageStoreService.storeIfOffline(USER_ID, MESSAGE_ID, PUSH_CHANNEL, false);

            ArgumentCaptor<OfflineMessage> captor = ArgumentCaptor.forClass(OfflineMessage.class);
            verify(offlineMessageRepository).save(captor.capture());

            OfflineMessage saved = captor.getValue();
            assertThat(saved.userId()).isEqualTo(USER_ID);
            assertThat(saved.messageId()).isEqualTo(MESSAGE_ID);
            assertThat(saved.pushChannel()).isEqualTo(PUSH_CHANNEL);
            assertThat(saved.pushStatus()).isEqualTo(PushStatus.PENDING);
        }

        @Test
        @DisplayName("用户在线时不存储离线消息")
        void storeIfOffline_userOnline_doesNotSave() {
            offlineMessageStoreService.storeIfOffline(USER_ID, MESSAGE_ID, PUSH_CHANNEL, true);

            verify(offlineMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("用户离线时使用默认重试计数值")
        void storeIfOffline_offline_setsDefaultRetryCount() {
            offlineMessageStoreService.storeIfOffline(USER_ID, MESSAGE_ID, PUSH_CHANNEL, false);

            ArgumentCaptor<OfflineMessage> captor = ArgumentCaptor.forClass(OfflineMessage.class);
            verify(offlineMessageRepository).save(captor.capture());

            OfflineMessage saved = captor.getValue();
            assertThat(saved.retryCount()).isEqualTo(MessageConstant.DEFAULT_RETRY_COUNT);
            assertThat(saved.maxRetryCount()).isEqualTo(MessageConstant.DEFAULT_MAX_RETRY_COUNT);
        }

        @Test
        @DisplayName("用户离线时可处理 null 参数")
        void storeIfOffline_offlineWithNullParams() {
            offlineMessageStoreService.storeIfOffline(null, null, null, false);

            ArgumentCaptor<OfflineMessage> captor = ArgumentCaptor.forClass(OfflineMessage.class);
            verify(offlineMessageRepository).save(captor.capture());

            OfflineMessage saved = captor.getValue();
            assertThat(saved.userId()).isNull();
            assertThat(saved.messageId()).isNull();
            assertThat(saved.pushChannel()).isNull();
        }
    }

    @Nested
    @DisplayName("replayPending")
    class ReplayPendingTests {

        @Test
        @DisplayName("无待推送消息时不动作")
        void replayPending_empty_noop() {
            when(offlineMessageRepository.findPendingByUserId(USER_ID)).thenReturn(List.of());

            offlineMessageStoreService.replayPending(USER_ID);

            verify(messageNotifier, never()).sendNotification(any(), any());
            verify(offlineMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("系统通知补推并标记 PUSHED")
        void replayPending_systemMessage_pushedAndMarked() {
            OfflineMessage pending = OfflineMessage.create(USER_ID, MESSAGE_ID, PUSH_CHANNEL);
            Message system = Message.createSystem(USER_ID, "收藏降价提醒", "价格已下降", "prod-1");
            when(offlineMessageRepository.findPendingByUserId(USER_ID)).thenReturn(List.of(pending));
            when(messageQueryRepository.findById(MESSAGE_ID)).thenReturn(system);

            offlineMessageStoreService.replayPending(USER_ID);

            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(messageNotifier).sendNotification(eq(USER_ID), payloadCaptor.capture());
            assertThat(payloadCaptor.getValue()).containsEntry("title", "收藏降价提醒");
            assertThat(payloadCaptor.getValue()).containsEntry("type", 1);

            ArgumentCaptor<OfflineMessage> savedCaptor = ArgumentCaptor.forClass(OfflineMessage.class);
            verify(offlineMessageRepository).save(savedCaptor.capture());
            assertThat(savedCaptor.getValue().pushStatus()).isEqualTo(PushStatus.PUSHED);
        }

        @Test
        @DisplayName("原消息已不存在时跳过（不推送不标记）")
        void replayPending_missingMessage_skipped() {
            OfflineMessage pending = OfflineMessage.create(USER_ID, MESSAGE_ID, PUSH_CHANNEL);
            when(offlineMessageRepository.findPendingByUserId(USER_ID)).thenReturn(List.of(pending));
            when(messageQueryRepository.findById(MESSAGE_ID)).thenReturn(null);

            offlineMessageStoreService.replayPending(USER_ID);

            verify(messageNotifier, never()).sendNotification(any(), any());
            verify(offlineMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("聊天消息不补推（会话数据由客户端拉取）")
        void replayPending_chatMessage_skipped() {
            OfflineMessage pending = OfflineMessage.create(USER_ID, MESSAGE_ID, PUSH_CHANNEL);
            Message chat = Message.create(USER_ID, "receiver-2", MessageType.CHAT, "在吗", "你好", null);
            when(offlineMessageRepository.findPendingByUserId(USER_ID)).thenReturn(List.of(pending));
            when(messageQueryRepository.findById(MESSAGE_ID)).thenReturn(chat);

            offlineMessageStoreService.replayPending(USER_ID);

            verify(messageNotifier, never()).sendNotification(any(), any());
            verify(offlineMessageRepository, never()).save(any());
        }
    }
}
