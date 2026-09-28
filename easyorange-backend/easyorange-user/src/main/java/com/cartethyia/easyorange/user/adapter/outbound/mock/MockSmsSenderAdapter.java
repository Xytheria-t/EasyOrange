package com.cartethyia.easyorange.user.adapter.outbound.mock;

import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 日志短信发送适配器 — {@link SmsSenderPort} 的唯一实现，验证码只落日志，不真实投递。
 * <p>
 * 取舍：接入真实短信供应商需要账号、签名与费用，仓内不保留供应商 SDK，端口留着——
 * 真换供应商时只新增一个实现类，不动调用方。
 * <p>
 * 边界：{@code RedisSmsCodeAdapter}（prod 生效）强依赖本端口，若此处不装配 prod，
 * 激活 prod 会在启动期抛 {@code NoSuchBeanDefinitionException}。因此 prod 也注册本实现，
 * 代价是「启动可用、业务不可用」——验证码确实发不出去。取舍理由：启动即失败是硬缺陷，
 * 短信通道对本仓库（不对外运营、验证码仅作流程占位）是次要的。
 */
@Slf4j
@Component
@Profile({"dev", "test", "default", "it", "prod"})
public class MockSmsSenderAdapter implements SmsSenderPort {

    @PostConstruct
    void warnNoRealProvider() {
        log.warn("[SMS] 未接入真实短信供应商：验证码只写日志，不会真实投递，prod 亦装配本实现。");
    }

    @Override
    public void send(String phone, String code) {
        log.info("══════════════════════════════════════════");
        log.info("  [MOCK SMS] 验证码发送");
        log.info("  手机号: {}", phone);
        log.info("  验证码: {}", code);
        log.info("  提示:   当前为模拟模式，不会真实发送短信");
        log.info("══════════════════════════════════════════");
    }
}
