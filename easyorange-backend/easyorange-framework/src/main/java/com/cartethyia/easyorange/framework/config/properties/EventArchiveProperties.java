package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 事件归档配置属性
 *
 * <p>控制 Spring Modulith {@code EVENT_PUBLICATION} 中已完成事件的归档时机：
 * 超过保留天数的 COMPLETED 行由 {@code EventPublicationArchiveTask} 搬入
 * {@code EVENT_PUBLICATION_ARCHIVE} 后从源表删除（同事务）。</p>
 *
 * @param archiveAfterDays 已完成事件在源表的保留天数，超期归档
 */
@Validated
@ConfigurationProperties(prefix = "easyorange.events")
public record EventArchiveProperties(@Min(1) @DefaultValue("7") int archiveAfterDays) {}
