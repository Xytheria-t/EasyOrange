package com.cartethyia.easyorange.framework.config.properties;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式锁配置 — 由 {@code DistributedRedissonLockAdapter} 消费。
 * <p>
 * watchdog 续期意味着事务卡住时锁不会自动释放，因此靠「持有时长告警」兜底：
 * 持有超过 {@link #getHoldWarnThreshold()} 即打 warn 日志，供监控/运维介入。
 */
@Data
@ConfigurationProperties(prefix = "easyorange.lock")
public class LockProperties {

    /** 锁持有超过该时长即告警（疑似事务长时间卡住）。 */
    private Duration holdWarnThreshold = Duration.ofSeconds(60);
}
