package com.cartethyia.easyorange.message.adapter.inbound.job;

import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.message.adapter.inbound.config.MessageRetentionProperties;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageDO;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageMapper;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageArchiveTask 单元测试")
class MessageArchiveTaskTest {

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private MessageArchiveBatchHandler archiveBatchHandler;

    private MessageArchiveTask archiveTask;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        archiveTask = new MessageArchiveTask(messageMapper, archiveBatchHandler, new MessageRetentionProperties());
    }

    @Nested
    @DisplayName("cleanupExpiredMessages")
    class CleanupExpiredMessagesTests {

        @Test
        @DisplayName("清理阈值含宽限期（retentionDays + cleanupGraceDays），保证归档先于物理删除")
        void cleanupExpiredMessages_cutoff_includesGraceDays() {
            when(messageMapper.deleteMessagesBefore(any())).thenReturn(0);
            LocalDateTime before = LocalDateTime.now();

            archiveTask.cleanupExpiredMessages();

            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(messageMapper).deleteMessagesBefore(captor.capture());
            // 默认 90 + 35 = 125 天前（允许 5s 误差）
            org.assertj.core.api.Assertions.assertThat(captor.getValue())
                    .isCloseTo(before.minusDays(125), within(5, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("有过期消息时分批清理直到删完")
        void cleanupExpiredMessages_hasExpired_deletesUntilEmpty() {
            when(messageMapper.deleteMessagesBefore(any())).thenReturn(1000, 1000, 0);

            archiveTask.cleanupExpiredMessages();

            verify(messageMapper, times(3)).deleteMessagesBefore(any());
        }

        @Test
        @DisplayName("mapper 抛出异常时被捕获不传播")
        void cleanupExpiredMessages_mapperThrows_caught() {
            when(messageMapper.deleteMessagesBefore(any())).thenThrow(new RuntimeException("DB error"));

            archiveTask.cleanupExpiredMessages();

            verify(messageMapper, times(1)).deleteMessagesBefore(any());
        }
    }

    @Nested
    @DisplayName("archiveOldMessages")
    class ArchiveOldMessagesTests {

        @Test
        @DisplayName("有待归档消息时按批交给事务处理器（归档+物理删除原子）")
        void archiveOldMessages_hasMessages_delegatesBatchAtomically() {
            MessageDO msg1 = MessageDO.builder()
                    .id("1")
                    .senderId("1")
                    .receiverId("2")
                    .type(1)
                    .title("title")
                    .content("content")
                    .isRead(ReadStatus.UNREAD)
                    .createTime(LocalDateTime.now().minusDays(100))
                    .build();
            MessageDO msg2 = MessageDO.builder()
                    .id("2")
                    .senderId("1")
                    .receiverId("2")
                    .type(1)
                    .title("title2")
                    .content("content2")
                    .isRead(ReadStatus.READ)
                    .createTime(LocalDateTime.now().minusDays(100))
                    .build();
            when(messageMapper.selectMessagesBefore(any()))
                    .thenReturn(List.of(msg1, msg2))
                    .thenReturn(List.of());

            archiveTask.archiveOldMessages();

            verify(messageMapper, times(2)).selectMessagesBefore(any());
            verify(archiveBatchHandler).archiveBatch(List.of(msg1, msg2));
        }

        @Test
        @DisplayName("无待归档消息时跳过")
        void archiveOldMessages_noMessages_skips() {
            when(messageMapper.selectMessagesBefore(any())).thenReturn(List.of());

            archiveTask.archiveOldMessages();

            verify(messageMapper, times(1)).selectMessagesBefore(any());
            verify(archiveBatchHandler, never()).archiveBatch(any());
        }

        @Test
        @DisplayName("多批次归档时循环处理")
        void archiveOldMessages_multipleBatches_processesAll() {
            MessageDO msg1 = MessageDO.builder()
                    .id("1")
                    .senderId("1")
                    .receiverId("2")
                    .type(1)
                    .title("t")
                    .content("c")
                    .isRead(ReadStatus.UNREAD)
                    .createTime(LocalDateTime.now().minusDays(100))
                    .build();
            MessageDO msg2 = MessageDO.builder()
                    .id("2")
                    .senderId("1")
                    .receiverId("2")
                    .type(1)
                    .title("t2")
                    .content("c2")
                    .isRead(ReadStatus.READ)
                    .createTime(LocalDateTime.now().minusDays(100))
                    .build();
            when(messageMapper.selectMessagesBefore(any()))
                    .thenReturn(List.of(msg1))
                    .thenReturn(List.of(msg2))
                    .thenReturn(List.of());

            archiveTask.archiveOldMessages();

            verify(archiveBatchHandler, times(2)).archiveBatch(any());
        }

        @Test
        @DisplayName("selectMessagesBefore 抛出异常时被捕获不传播")
        void archiveOldMessages_mapperThrows_caught() {
            when(messageMapper.selectMessagesBefore(any())).thenThrow(new RuntimeException("DB error"));

            archiveTask.archiveOldMessages();

            verify(messageMapper, times(1)).selectMessagesBefore(any());
            verify(archiveBatchHandler, never()).archiveBatch(any());
        }
    }

    @Nested
    @DisplayName("MessageArchiveBatchHandler")
    class BatchHandlerTests {

        @Test
        @DisplayName("归档写入与按 ID 物理删除在同一调用内成对执行")
        void archiveBatch_insertsArchive_thenPhysicalDeletes() {
            MessageDO msg = MessageDO.builder()
                    .id("1")
                    .senderId("1")
                    .receiverId("2")
                    .type(1)
                    .title("t")
                    .content("c")
                    .isRead(ReadStatus.UNREAD)
                    .createTime(LocalDateTime.now().minusDays(100))
                    .build();
            MessageArchiveBatchHandler handler = new MessageArchiveBatchHandler(messageMapper);

            handler.archiveBatch(List.of(msg));

            verify(messageMapper).batchInsertArchive(List.of(msg));
            verify(messageMapper).deleteByIdsPhysical(List.of("1"));
        }
    }
}
