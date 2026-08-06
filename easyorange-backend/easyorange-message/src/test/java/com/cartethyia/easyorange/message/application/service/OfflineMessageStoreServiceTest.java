package com.cartethyia.easyorange.message.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;
import com.cartethyia.easyorange.message.domain.constant.MessageConstant;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
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
            assertThat(saved.pushStatus()).isEqualTo(MessageConstant.PUSH_STATUS_PENDING);
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
}
