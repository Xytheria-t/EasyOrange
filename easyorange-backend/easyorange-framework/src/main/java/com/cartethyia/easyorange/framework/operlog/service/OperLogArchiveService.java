package com.cartethyia.easyorange.framework.operlog.service;

import com.cartethyia.easyorange.framework.operlog.entity.SysOperLog;
import com.cartethyia.easyorange.framework.operlog.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志归档服务
 * <p>
 * 提供定时清理和归档功能：
 * - 每天凌晨 3 点清理超过保留天数的日志
 * - 每月 1 号凌晨 2 点将旧日志归档到 sys_oper_log_archive 表
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperLogArchiveService {

    private final SysOperLogMapper sysOperLogMapper;

    @Value("${oper-log.retention-days:90}")
    private int retentionDays;

    /**
     * 每天凌晨 3 点执行日志清理
     * 删除超过保留天数的操作日志
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredLogs() {
        try {
            LocalDateTime expireDate = LocalDateTime.now().minusDays(retentionDays);
            int deletedCount = sysOperLogMapper.deleteExpiredLogs(expireDate);
            log.info("Cleaned up {} expired operation logs (older than {} days)",
                    deletedCount, retentionDays);
        } catch (Exception e) {
            log.error("Failed to cleanup expired operation logs", e);
        }
    }

    /**
     * 每月 1 号凌晨 2 点执行归档
     * 将超过保留天数的日志迁移到 sys_oper_log_archive 表后删除
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional
    public void archiveOldLogs() {
        try {
            LocalDateTime archiveDate = LocalDateTime.now().minusDays(retentionDays);
            int totalArchived = 0;
            int batchCount = 0;

            while (true) {
                List<SysOperLog> logsToArchive = sysOperLogMapper.selectLogsBefore(archiveDate);
                if (logsToArchive.isEmpty()) {
                    break;
                }

                // 批量插入归档表
                sysOperLogMapper.batchInsertArchive(logsToArchive);
                totalArchived += logsToArchive.size();
                batchCount++;

                // 删除已归档的记录
                for (SysOperLog logItem : logsToArchive) {
                    sysOperLogMapper.deleteById(logItem.getOperId());
                }

                log.info("Archived batch #{}: {} logs", batchCount, logsToArchive.size());
            }

            if (totalArchived > 0) {
                log.info("Monthly archive completed: {} logs archived in {} batches",
                        totalArchived, batchCount);
            }
        } catch (Exception e) {
            log.error("Failed to archive old operation logs", e);
        }
    }
}
