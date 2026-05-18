package com.cartethyia.easyorange.message.service;

import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.message.entity.Message;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageArchiveService {

    private final MessageMapper messageMapper;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Value("${easyorange.message.retention-days:90}")
    private int retentionDays;

    private static final int BATCH_SIZE = 1000;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredMessages() {
        try {
            LocalDateTime expireDate = LocalDateTime.now().minusDays(retentionDays);
            int totalDeleted = 0;
            int deleted;

            do {
                deleted = deleteExpiredBatch(expireDate);
                totalDeleted += deleted;
            } while (deleted > 0);

            log.info("Cleaned up {} expired messages (older than {} days)", totalDeleted, retentionDays);
        } catch (Exception e) {
            log.error("Failed to cleanup expired messages", e);
        }
    }

    @Scheduled(cron = "0 0 2 1 * ?")
    public void archiveOldMessages() {
        try {
            LocalDateTime archiveDate = LocalDateTime.now().minusDays(retentionDays);
            int totalArchived = 0;
            int batchCount = 0;

            while (true) {
                List<Message> messagesToArchive = selectMessagesBefore(archiveDate);
                if (messagesToArchive.isEmpty()) {
                    break;
                }

                batchInsertArchive(messagesToArchive);
                totalArchived += messagesToArchive.size();
                batchCount++;

                List<Long> idsToDelete = messagesToArchive.stream()
                        .map(Message::getId)
                        .toList();
                deleteByIds(idsToDelete);

                log.info("Archived batch #{}: {} messages", batchCount, messagesToArchive.size());
            }

            if (totalArchived > 0) {
                log.info("Monthly archive completed: {} messages archived in {} batches", totalArchived, batchCount);
            }
        } catch (Exception e) {
            log.error("Failed to archive old messages", e);
        }
    }

    private List<Message> selectMessagesBefore(LocalDateTime targetDate) {
        return ChainWrappers.lambdaQueryChain(messageMapper)
                .lt(Message::getCreateTime, targetDate)
                .eq(Message::getDelFlag, 0)
                .orderByAsc(Message::getCreateTime)
                .last("LIMIT " + BATCH_SIZE)
                .list();
    }

    private void batchInsertArchive(List<Message> messages) {
        if (messages.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO eo_message_archive
            (id, sender_id, receiver_id, type, title, content, is_read, read_time,
             business_id, conversation_id, create_time, update_time, create_by, update_by)
            VALUES
            (:id, :senderId, :receiverId, :type, :title, :content, :isRead, :readTime,
             :businessId, :conversationId, :createTime, :updateTime, :createBy, :updateBy)
            """;

        MapSqlParameterSource[] batchParams = messages.stream()
                .map(msg -> new MapSqlParameterSource()
                        .addValue("id", msg.getId())
                        .addValue("senderId", msg.getSenderId())
                        .addValue("receiverId", msg.getReceiverId())
                        .addValue("type", msg.getType())
                        .addValue("title", msg.getTitle())
                        .addValue("content", msg.getContent())
                        .addValue("isRead", msg.getIsRead())
                        .addValue("readTime", msg.getReadTime())
                        .addValue("businessId", msg.getBusinessId())
                        .addValue("conversationId", msg.getConversationId())
                        .addValue("createTime", msg.getCreateTime())
                        .addValue("updateTime", msg.getUpdateTime())
                        .addValue("createBy", msg.getCreateBy())
                        .addValue("updateBy", msg.getUpdateBy()))
                .toArray(MapSqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(sql, batchParams);
    }

    private void deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        messageMapper.deleteBatchIds(ids);
    }

    private int deleteExpiredBatch(LocalDateTime expireDate) {
        return ChainWrappers.lambdaUpdateChain(messageMapper)
                .lt(Message::getCreateTime, expireDate)
                .last("LIMIT " + BATCH_SIZE)
                .remove() ? 1 : 0;
    }
}
