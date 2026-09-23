package com.cartethyia.easyorange.framework.event.archive;

import com.cartethyia.easyorange.framework.config.properties.EventArchiveProperties;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事件归档任务 — 每日把超期的 COMPLETED 事件从 {@code EVENT_PUBLICATION} 搬入
 * {@code EVENT_PUBLICATION_ARCHIVE}，防源表无限积压（Modulith {@code completion-mode: update}
 * 只更新 COMPLETION_DATE 不删行，没有本任务已完成事件将永久驻留）。
 * <p>
 * 同事务先 INSERT…SELECT 再 DELETE（同一 WHERE），搬运要么整体成功要么整体不发生；
 * 异常上抛由调度器记录并回滚 —— 方法内不 catch，否则「归档已插、源未删」的半提交会让
 * 下一轮搬运撞主键永久卡死。
 * <p>
 * 演示数据量下超期行是百级，单批搬完即可，不做分批（量级起来再按审计清理任务的
 * LIMIT 循环样式拆批）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublicationArchiveTask {

    /** 与 {@code spring.modulith.events.completion-mode: update} 配套：完成但超期即搬走 */
    private static final String WHERE_COMPLETED_BEFORE = "WHERE STATUS = 'COMPLETED' AND COMPLETION_DATE < ?";

    private static final String COLUMNS = "ID, LISTENER_ID, EVENT_TYPE, SERIALIZED_EVENT, PUBLICATION_DATE, "
            + "COMPLETION_DATE, STATUS, COMPLETION_ATTEMPTS, LAST_RESUBMISSION_DATE";

    private final JdbcTemplate jdbcTemplate;
    private final EventArchiveProperties properties;

    /** 每日 03:30 归档（与审计清理 03:00 错峰）。 */
    @Scheduled(cron = "0 30 3 * * ?")
    @Transactional
    public void archiveCompletedEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(properties.archiveAfterDays());
        int moved = jdbcTemplate.update(
                "INSERT INTO EVENT_PUBLICATION_ARCHIVE (" + COLUMNS + ") " + "SELECT " + COLUMNS
                        + " FROM EVENT_PUBLICATION " + WHERE_COMPLETED_BEFORE,
                cutoff);
        if (moved > 0) {
            jdbcTemplate.update("DELETE FROM EVENT_PUBLICATION " + WHERE_COMPLETED_BEFORE, cutoff);
            log.info("事件归档完成: 搬运 {} 条 COMPLETED 事件至 ARCHIVE (保留 {} 天)", moved, properties.archiveAfterDays());
        }
    }
}
