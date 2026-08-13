package com.cartethyia.easyorange.framework.audit.job;

import com.cartethyia.easyorange.framework.audit.mapper.AuditLogMapper;
import com.cartethyia.easyorange.framework.config.properties.AuditLogProperties;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 审计日志保留期限清理任务 — 每日删除超过 {@code audit.retention-days} 的审计记录。
 * <p>
 * 分批删除（每次 {@code LIMIT 1000}，循环直至删完），避免单条大 DELETE 长时间持锁；
 * 与 message 模块 {@code MessageArchiveTask} 的清理惯例对齐。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogCleanupTask {

    private final AuditLogMapper auditLogMapper;
    private final AuditLogProperties auditLogProperties;

    /** 每日 03:00 清理过期审计日志（分批 DELETE ... LIMIT 1000）。 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredLogs() {
        try {
            LocalDateTime expireDate = LocalDateTime.now().minusDays(auditLogProperties.getRetentionDays());
            int totalDeleted = 0;
            int deleted;
            do {
                deleted = auditLogMapper.deleteExpiredLogs(expireDate);
                totalDeleted += deleted;
            } while (deleted > 0);

            log.info("审计日志清理完成: 删除 {} 条 (保留 {} 天)", totalDeleted, auditLogProperties.getRetentionDays());
        } catch (Exception e) {
            log.error("审计日志清理失败", e);
        }
    }
}
