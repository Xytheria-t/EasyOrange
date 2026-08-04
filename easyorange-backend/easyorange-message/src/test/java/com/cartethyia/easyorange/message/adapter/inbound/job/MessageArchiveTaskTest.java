package com.cartethyia.easyorange.message.adapter.inbound.job;

import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageDO;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageMapper;
import com.cartethyia.easyorange.message.enums.ReadStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageArchiveTask 单元测试")
class MessageArchiveTaskTest {

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private MessageArchiveTask archiveTask;

    @Nested
    @DisplayName("cleanupExpiredMessages")
    class CleanupExpiredMessagesTests {

        @Test
        @DisplayName("有过期消息时分批清理直到删完")
        void cleanupExpiredMessages_hasExpired_deletesUntilEmpty() {
            when(messageMapper.deleteMessagesBefore(any())).thenReturn(1000, 1000, 0);

            archiveTask.cleanupExpiredMessages();

            verify(messageMapper, times(3)).deleteMessagesBefore(any());
        }

        @Test
        @DisplayName("无过期消息时只查一次")
        void cleanupExpiredMessages_noExpired_queriesOnce() {
            when(messageMapper.deleteMessagesBefore(any())).thenReturn(0);

            archiveTask.cleanupExpiredMessages();

            verify(messageMapper, times(1)).deleteMessagesBefore(any());
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
        @DisplayName("有待归档消息时归档并删除原记录")
        void archiveOldMessages_hasMessages_archivesAndDeletes() {
            MessageDO msg1 = MessageDO.builder().id("1").senderId("1").receiverId("2").type(1)
                    .title("title").content("content").isRead(ReadStatus.UNREAD)
                    .createTime(LocalDateTime.now().minusDays(100)).build();
            MessageDO msg2 = MessageDO.builder().id("2").senderId("1").receiverId("2").type(1)
                    .title("title2").content("content2").isRead(ReadStatus.READ)
                    .createTime(LocalDateTime.now().minusDays(100)).build();
            when(messageMapper.selectMessagesBefore(any()))
                    .thenReturn(List.of(msg1, msg2))
                    .thenReturn(List.of());

            archiveTask.archiveOldMessages();

            verify(messageMapper, times(2)).selectMessagesBefore(any());
            verify(messageMapper).batchInsertArchive(any());
            verify(messageMapper).deleteByIds(List.of("1", "2"));
        }

        @Test
        @DisplayName("无待归档消息时跳过")
        void archiveOldMessages_noMessages_skips() {
            when(messageMapper.selectMessagesBefore(any())).thenReturn(List.of());

            archiveTask.archiveOldMessages();

            verify(messageMapper, times(1)).selectMessagesBefore(any());
            verify(messageMapper, never()).batchInsertArchive(any());
            verify(messageMapper, never()).deleteByIds(any());
        }

        @Test
        @DisplayName("多批次归档时循环处理")
        void archiveOldMessages_multipleBatches_processesAll() {
            MessageDO msg1 = MessageDO.builder().id("1").senderId("1").receiverId("2").type(1)
                    .title("t").content("c").isRead(ReadStatus.UNREAD)
                    .createTime(LocalDateTime.now().minusDays(100)).build();
            MessageDO msg2 = MessageDO.builder().id("2").senderId("1").receiverId("2").type(1)
                    .title("t2").content("c2").isRead(ReadStatus.READ)
                    .createTime(LocalDateTime.now().minusDays(100)).build();
            when(messageMapper.selectMessagesBefore(any()))
                    .thenReturn(List.of(msg1))
                    .thenReturn(List.of(msg2))
                    .thenReturn(List.of());

            archiveTask.archiveOldMessages();

            verify(messageMapper, times(2)).batchInsertArchive(any());
            verify(messageMapper, times(2)).deleteByIds(any());
        }

        @Test
        @DisplayName("selectMessagesBefore 抛出异常时被捕获不传播")
        void archiveOldMessages_mapperThrows_caught() {
            when(messageMapper.selectMessagesBefore(any())).thenThrow(new RuntimeException("DB error"));

            archiveTask.archiveOldMessages();

            verify(messageMapper, times(1)).selectMessagesBefore(any());
            verify(messageMapper, never()).batchInsertArchive(any());
        }
    }
}
