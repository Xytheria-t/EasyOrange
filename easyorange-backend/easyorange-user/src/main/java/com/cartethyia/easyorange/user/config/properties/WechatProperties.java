package com.cartethyia.easyorange.user.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信配置属性
 *
 * @author cartethyia
 * @date 2026/03/07
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.miniprogram")
public class WechatProperties {

    /**
     * 小程序 AppID
     */
    private String appId;

    /**
     * 小程序 AppSecret
     */
    private String appSecret;
}
