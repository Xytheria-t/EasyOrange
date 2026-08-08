package com.cartethyia.easyorange.message.adapter.inbound.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "easyorange.message")
public class MessageRetentionProperties {

    /** 消息保留天数，超期由 MessageArchiveTask 每日清理。 */
    private int retentionDays = 90;
}
