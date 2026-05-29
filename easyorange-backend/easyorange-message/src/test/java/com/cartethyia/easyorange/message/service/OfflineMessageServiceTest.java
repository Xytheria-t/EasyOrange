package com.cartethyia.easyorange.message.service;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessageAggregate;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
import com.cartethyia.easyorange.message.service.impl.OfflineMessageServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OfflineMessageService 单元测试")
class OfflineMessageServiceTest {

    @Mock
    private OfflineMessageRepository offlineMessageRepository;

    @InjectMocks
    private OfflineMessageServiceImpl offlineMessageService;

    private static final Long USER_ID = 1L;
    private static final Long MESSAGE_ID = 100L;
    private static final Long OFFLINE_MESSAGE_ID = 50L;
    private static final String PUSH_CHANNEL = "WEBSOCKET";

    @Nested
    @DisplayName("saveOfflineMessage")
    class SaveOfflineMessageTests {

        @Test
        @DisplayName("保存离线消息")
        void saveOfflineMessage_saves() {
            offlineMessageService.saveOfflineMessage(USER_ID, MESSAGE_ID, PUSH_CHANNEL);

            ArgumentCaptor<OfflineMessageAggregate> captor = ArgumentCaptor.forClass(OfflineMessageAggregate.class);
            verify(offlineMessageRepository).save(captor.capture());

            OfflineMessageAggregate saved = captor.getValue();
            assertThat(saved.userId()).isEqualTo(USER_ID);
            assertThat(saved.messageId()).isEqualTo(MESSAGE_ID);
            assertThat(saved.pushChannel()).isEqualTo(PUSH_CHANNEL);
        }
    }

    @Nested
    @DisplayName("getPendingMessages")
    class GetPendingMessagesTests {

        @Test
        @DisplayName("获取用户的待推送消息")
        void getPendingMessages_returnsList() {
            OfflineMessageAggregate msg = OfflineMessageAggregate.fromRaw(
                    OFFLINE_MESSAGE_ID, USER_ID, MESSAGE_ID, PUSH_CHANNEL, 0, 0, 3);
            when(offlineMessageRepository.findPendingByUserId(USER_ID)).thenReturn(List.of(msg));

            List<OfflineMessageAggregate> result = offlineMessageService.getPendingMessages(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).userId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("无待推送消息时返回空列表")
        void getPendingMessages_noPending_returnsEmpty() {
            when(offlineMessageRepository.findPendingByUserId(USER_ID)).thenReturn(List.of());

            List<OfflineMessageAggregate> result = offlineMessageService.getPendingMessages(USER_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("markAsPushed")
    class MarkAsPushedTests {

        @Test
        @DisplayName("标记离线消息为已推送")
        void markAsPushed_marks() {
            offlineMessageService.markAsPushed(OFFLINE_MESSAGE_ID);

            verify(offlineMessageRepository).markAsPushed(OFFLINE_MESSAGE_ID);
        }
    }

    @Nested
    @DisplayName("markAsFailed")
    class MarkAsFailedTests {

        @Test
        @DisplayName("标记离线消息为推送失败")
        void markAsFailed_marks() {
            offlineMessageService.markAsFailed(OFFLINE_MESSAGE_ID);

            verify(offlineMessageRepository).markAsFailed(OFFLINE_MESSAGE_ID);
        }
    }

    @Nested
    @DisplayName("incrementRetryCount")
    class IncrementRetryCountTests {

        @Test
        @DisplayName("递增重试计数")
        void incrementRetryCount_increments() {
            offlineMessageService.incrementRetryCount(OFFLINE_MESSAGE_ID);

            verify(offlineMessageRepository).incrementRetryCount(OFFLINE_MESSAGE_ID);
        }
    }
}
