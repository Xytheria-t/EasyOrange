package com.cartethyia.easyorange.message.domain.service;

import com.cartethyia.easyorange.message.constant.MessageConstant;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
import com.cartethyia.easyorange.message.entity.OfflineMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OfflineMessageStoreService 单元测试")
class OfflineMessageStoreServiceTest {

    @Mock
    private OfflineMessageRepository offlineMessageRepository;

    @InjectMocks
    private OfflineMessageStoreService offlineMessageStoreService;

    private static final Long USER_ID = 1L;
    private static final Long MESSAGE_ID = 100L;
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
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getMessageId()).isEqualTo(MESSAGE_ID);
            assertThat(saved.getPushChannel()).isEqualTo(PUSH_CHANNEL);
            assertThat(saved.getPushStatus()).isEqualTo(MessageConstant.PUSH_STATUS_PENDING);
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
            assertThat(saved.getRetryCount()).isEqualTo(MessageConstant.DEFAULT_RETRY_COUNT);
            assertThat(saved.getMaxRetryCount()).isEqualTo(MessageConstant.DEFAULT_MAX_RETRY_COUNT);
        }

        @Test
        @DisplayName("用户离线时可处理 null 参数")
        void storeIfOffline_offlineWithNullParams() {
            offlineMessageStoreService.storeIfOffline(null, null, null, false);

            ArgumentCaptor<OfflineMessage> captor = ArgumentCaptor.forClass(OfflineMessage.class);
            verify(offlineMessageRepository).save(captor.capture());

            OfflineMessage saved = captor.getValue();
            assertThat(saved.getUserId()).isNull();
            assertThat(saved.getMessageId()).isNull();
            assertThat(saved.getPushChannel()).isNull();
        }
    }
}
