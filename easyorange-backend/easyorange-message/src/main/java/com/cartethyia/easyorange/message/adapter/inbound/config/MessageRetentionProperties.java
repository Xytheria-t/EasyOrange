package com.cartethyia.easyorange.message.adapter.inbound.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "easyorange.message")
public record MessageRetentionProperties(
        /** 消息保留天数：超期消息由月度归档任务搬入 eo_message_archive 后从主表删除。 */
        @Min(1) @DefaultValue("90") int retentionDays,

        /**
         * 清理宽限天数：每日兜底清理只物理删除 retentionDays + cleanupGraceDays 之前的消息。
         * 宽限期须大于归档周期（月，最长 31 天），保证归档任务总是先于物理删除看到超期消息。
         */
        @Min(1) @DefaultValue("35") int cleanupGraceDays) {}
