package com.cartethyia.easyorange.user.adapter.outbound.mock;

import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 模拟短信发送适配器（开发/测试环境）。
 * <p>
 * 不真实调用短信服务商，而是将验证码打印到日志，方便开发调试。
 * <p>
 * 生产环境替换为真实 SMS 发送实现。
 */
@Slf4j
@Component
@Profile({"dev", "test", "default"})
public class MockSmsSenderAdapter implements SmsSenderPort {

    @Override
    public void send(String phone, String code) {
        log.info("══════════════════════════════════════════");
        log.info("  [MOCK SMS] 验证码发送");
        log.info("  手机号: {}", phone);
        log.info("  验证码: {}", code);
        log.info("  提示:   当前为模拟模式，不会真实发送短信");
        log.info("  提示:   生产环境请配置真实的 SmsSenderPort 实现");
        log.info("══════════════════════════════════════════");
    }
}
