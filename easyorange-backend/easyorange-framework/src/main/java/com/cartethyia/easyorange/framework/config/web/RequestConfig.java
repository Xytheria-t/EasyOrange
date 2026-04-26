package com.cartethyia.easyorange.framework.config.web;

import com.cartethyia.easyorange.common.util.RequestUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * 请求配置类
 * <p>
 * 初始化信任的代理 IP 列表，用于反向代理场景下正确获取客户端真实 IP
 * </p>
 *
 * @author cartethyia
 */
@Slf4j
@Configuration
public class RequestConfig {

    @Value("${server.trusted-proxies:127.0.0.1,localhost,0:0:0:0:0:0:0:1}")
    private String[] trustedProxies;

    @PostConstruct
    public void init() {
        RequestUtil.setTrustedProxies(trustedProxies);
        log.info("已配置信任的代理 IP 列表：{}", Arrays.toString(trustedProxies));
    }
}
