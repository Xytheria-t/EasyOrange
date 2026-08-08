package com.cartethyia.easyorange.message.adapter.inbound.job;

import com.cartethyia.easyorange.message.adapter.inbound.config.MessageRetentionProperties;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageDO;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 过期消息清理/归档定时任务 — 入站适配器。
 * <p>
 * 持久化统一走 {@link MessageMapper} 的 XML 批处理语句（selectMessagesBefore/batchInsertArchive/
 * deleteMessagesBefore），与 order 模块 {@code adapter/inbound/job/} 的定时任务惯例对齐。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageArchiveTask {

    private final MessageMapper messageMapper;
    private final MessageRetentionProperties retentionProperties;

    /** 每日 03:00 清理过期消息（分批 DELETE ... LIMIT 1000）。 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredMessages() {
        try {
            LocalDateTime expireDate = LocalDateTime.now().minusDays(retentionProperties.getRetentionDays());
            int totalDeleted = 0;
            int deleted;
            do {
                deleted = messageMapper.deleteMessagesBefore(expireDate);
                totalDeleted += deleted;
            } while (deleted > 0);

            log.info("Cleaned up {} expired messages (older than {} days)", totalDeleted, retentionProperties.getRetentionDays());
        } catch (Exception e) {
            log.error("Failed to cleanup expired messages", e);
        }
    }

    /** 每月 1 日 02:00 将过期消息归档到 eo_message_archive 后从主表删除。 */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void archiveOldMessages() {
        try {
            LocalDateTime archiveDate = LocalDateTime.now().minusDays(retentionProperties.getRetentionDays());
            int totalArchived = 0;
            int batchCount = 0;

            while (true) {
                List<MessageDO> messagesToArchive = messageMapper.selectMessagesBefore(archiveDate);
                if (messagesToArchive.isEmpty()) {
                    break;
                }

                messageMapper.batchInsertArchive(messagesToArchive);
                totalArchived += messagesToArchive.size();
                batchCount++;

                messageMapper.deleteByIds(
                        messagesToArchive.stream().map(MessageDO::getId).toList());

                log.info("Archived batch #{}: {} messages", batchCount, messagesToArchive.size());
            }

            if (totalArchived > 0) {
                log.info("Monthly archive completed: {} messages archived in {} batches", totalArchived, batchCount);
            }
        } catch (Exception e) {
            log.error("Failed to archive old messages", e);
        }
    }
}
