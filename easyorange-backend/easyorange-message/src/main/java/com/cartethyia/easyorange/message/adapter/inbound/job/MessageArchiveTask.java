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
 * 归档先行：每月 1 日把超期消息按批原子搬入 {@code eo_message_archive}（见
 * {@link MessageArchiveBatchHandler}）；每日兜底清理只物理删除「保留期 + 宽限期」之前的
 * 消息，宽限期覆盖归档周期，保证归档任务总是先于物理删除看到超期数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageArchiveTask {

    private final MessageMapper messageMapper;
    private final MessageArchiveBatchHandler archiveBatchHandler;
    private final MessageRetentionProperties retentionProperties;

    /** 每日 03:00 兜底清理（分批 DELETE ... LIMIT 1000），只处理归档漏网的超期消息。 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredMessages() {
        try {
            LocalDateTime expireDate = LocalDateTime.now()
                    .minusDays(retentionProperties.getRetentionDays() + retentionProperties.getCleanupGraceDays());
            int totalDeleted = 0;
            int deleted;
            do {
                deleted = messageMapper.deleteMessagesBefore(expireDate);
                totalDeleted += deleted;
            } while (deleted > 0);

            log.info(
                    "Cleaned up {} expired messages (older than {} days)",
                    totalDeleted,
                    retentionProperties.getRetentionDays() + retentionProperties.getCleanupGraceDays());
        } catch (Exception e) {
            log.error("Failed to cleanup expired messages", e);
        }
    }

    /** 每月 1 日 02:00 将超期消息分批原子归档（写入归档表 + 主表物理删除同事务）。 */
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

                archiveBatchHandler.archiveBatch(messagesToArchive);
                totalArchived += messagesToArchive.size();
                batchCount++;

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
