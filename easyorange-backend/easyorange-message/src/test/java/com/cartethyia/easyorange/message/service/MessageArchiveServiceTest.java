package com.cartethyia.easyorange.message.service;

import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageMapper;
import com.cartethyia.easyorange.message.entity.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageArchiveService 单元测试")
class MessageArchiveServiceTest {

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @InjectMocks
    private MessageArchiveService archiveService;

    @Nested
    @DisplayName("cleanupExpiredMessages")
    class CleanupExpiredMessagesTests {

        @Test
        @DisplayName("有过期消息时清理并记录")
        void cleanupExpiredMessages_hasExpired_deletesAndLogs() {
            when(messageMapper.delete(any())).thenReturn(5, 0);

            archiveService.cleanupExpiredMessages();

            verify(messageMapper, times(2)).delete(any());
        }

        @Test
        @DisplayName("无过期消息时跳过清理")
        void cleanupExpiredMessages_noExpired_skips() {
            when(messageMapper.delete(any())).thenReturn(0);

            archiveService.cleanupExpiredMessages();

            verify(messageMapper, times(1)).delete(any());
        }

        @Test
        @DisplayName("mapper 抛出异常时被捕获不传播")
        void cleanupExpiredMessages_mapperThrows_caught() {
            when(messageMapper.delete(any())).thenThrow(new RuntimeException("DB error"));

            archiveService.cleanupExpiredMessages();

            verify(messageMapper, times(1)).delete(any());
        }
    }

    @Nested
    @DisplayName("archiveOldMessages")
    class ArchiveOldMessagesTests {

        @Test
        @DisplayName("有待归档消息时归档并删除原记录")
        void archiveOldMessages_hasMessages_archivesAndDeletes() {
            Message msg1 = Message.builder().id("1").senderId("1").receiverId("2").type(1)
                    .title("title").content("content").isRead(0)
                    .createTime(LocalDateTime.now().minusDays(100)).build();
            Message msg2 = Message.builder().id("2").senderId("1").receiverId("2").type(1)
                    .title("title2").content("content2").isRead(1)
                    .createTime(LocalDateTime.now().minusDays(100)).build();
            when(messageMapper.selectList(any())).thenReturn(List.of(msg1, msg2), List.of());

            archiveService.archiveOldMessages();

            verify(messageMapper, times(2)).selectList(any());
            verify(namedParameterJdbcTemplate).batchUpdate(anyString(), any(MapSqlParameterSource[].class));
            verify(messageMapper).deleteBatchIds(List.of("1", "2"));
        }

        @Test
        @DisplayName("无待归档消息时跳过")
        void archiveOldMessages_noMessages_skips() {
            when(messageMapper.selectList(any())).thenReturn(List.of());

            archiveService.archiveOldMessages();

            verify(messageMapper, times(1)).selectList(any());
            verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(MapSqlParameterSource[].class));
            verify(messageMapper, never()).deleteBatchIds(any());
        }

        @Test
        @DisplayName("多批次归档时循环处理")
        void archiveOldMessages_multipleBatches_processesAll() {
            Message msg1 = Message.builder().id("1").senderId("1").receiverId("2").type(1)
                    .title("t").content("c").isRead(0)
                    .createTime(LocalDateTime.now().minusDays(100)).build();
            Message msg2 = Message.builder().id("2").senderId("1").receiverId("2").type(1)
                    .title("t2").content("c2").isRead(1)
                    .createTime(LocalDateTime.now().minusDays(100)).build();
            when(messageMapper.selectList(any())).thenReturn(List.of(msg1), List.of(msg2), List.of());

            archiveService.archiveOldMessages();

            verify(namedParameterJdbcTemplate, times(2)).batchUpdate(anyString(), any(MapSqlParameterSource[].class));
            verify(messageMapper, times(2)).deleteBatchIds(any());
        }

        @Test
        @DisplayName("selectList 抛出异常时被捕获不传播")
        void archiveOldMessages_mapperThrows_caught() {
            when(messageMapper.selectList(any())).thenThrow(new RuntimeException("DB error"));

            archiveService.archiveOldMessages();

            verify(messageMapper, times(1)).selectList(any());
            verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(MapSqlParameterSource[].class));
        }
    }
}
