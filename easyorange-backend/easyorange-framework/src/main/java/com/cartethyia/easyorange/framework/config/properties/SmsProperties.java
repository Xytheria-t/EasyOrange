package com.cartethyia.easyorange.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 短信配置。
 *
 * @param demoCode 演示验证码 — 配了即固定用该码（免翻日志取码），留空走随机 6 位；只有 dev 的
 *                 {@code MockSmsCodeAdapter} 读它，it / prod 走 Redis 适配器不受影响
 */
@ConfigurationProperties(prefix = "easyorange.sms")
public record SmsProperties(@DefaultValue("") String demoCode) {}
