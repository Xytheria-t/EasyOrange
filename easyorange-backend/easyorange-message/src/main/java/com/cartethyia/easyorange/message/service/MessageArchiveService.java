package com.cartethyia.easyorange.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.message.entity.Message;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageArchiveService {

    private final MessageMapper messageMapper;
    private final JdbcTemplate jdbcTemplate;

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
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(Message::getCreateTime, targetDate)
                .eq(Message::getDelFlag, 0)
                .orderByAsc(Message::getCreateTime)
                .last("LIMIT " + BATCH_SIZE);
        return messageMapper.selectList(wrapper);
    }

    private void batchInsertArchive(List<Message> messages) {
        if (messages.isEmpty()) {
            return;
        }

        StringBuilder sql = new StringBuilder(
                "INSERT INTO eo_message_archive (id, sender_id, receiver_id, type, title, content, " +
                "is_read, read_time, business_id, conversation_id, create_time, update_time, create_by, update_by) VALUES "
        );

        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(String.format("(%d, %s, %d, %d, %s, %s, %d, %s, %s, %s, %s, %s, %s, %s)",
                    msg.getId(),
                    msg.getSenderId() != null ? msg.getSenderId().toString() : "NULL",
                    msg.getReceiverId(),
                    msg.getType() != null ? msg.getType() : 0,
                    msg.getTitle() != null ? "'" + escapeSql(msg.getTitle()) + "'" : "NULL",
                    "'" + escapeSql(msg.getContent()) + "'",
                    msg.getIsRead() != null ? msg.getIsRead() : 0,
                    msg.getReadTime() != null ? "'" + msg.getReadTime() + "'" : "NULL",
                    msg.getBusinessId() != null ? msg.getBusinessId().toString() : "NULL",
                    msg.getConversationId() != null ? msg.getConversationId().toString() : "NULL",
                    msg.getCreateTime() != null ? "'" + msg.getCreateTime() + "'" : "NULL",
                    msg.getUpdateTime() != null ? "'" + msg.getUpdateTime() + "'" : "NULL",
                    msg.getCreateBy() != null ? msg.getCreateBy().toString() : "NULL",
                    msg.getUpdateBy() != null ? msg.getUpdateBy().toString() : "NULL"
            ));
        }

        jdbcTemplate.execute(sql.toString());
    }

    private void deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        messageMapper.deleteBatchIds(ids);
    }

    private int deleteExpiredBatch(LocalDateTime expireDate) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(Message::getCreateTime, expireDate)
                .last("LIMIT " + BATCH_SIZE);
        return messageMapper.delete(wrapper);
    }

    private String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }
}
