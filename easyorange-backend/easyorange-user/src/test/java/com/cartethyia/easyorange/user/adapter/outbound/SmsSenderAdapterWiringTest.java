package com.cartethyia.easyorange.user.adapter.outbound;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.user.adapter.outbound.mock.MockSmsSenderAdapter;
import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * {@link SmsSenderPort} 装配测试 — 守一个回归：{@code RedisSmsCodeAdapter}（prod 生效）构造注入
 * 本端口，而仓内唯一实现早期未含 prod profile，激活 prod 直接 {@code NoSuchBeanDefinitionException} 启动失败。
 */
class SmsSenderAdapterWiringTest {

    @Test
    @DisplayName("prod → 注册日志发送器（真发短信前不得对外运营，但必须能启动）")
    void prodRegistersSender() {
        assertThat(smsSenders("prod")).hasSize(1);
    }

    @Test
    @DisplayName("dev / it → 注册日志发送器")
    void knownProfilesRegisterSender() {
        assertThat(smsSenders("dev")).hasSize(1);
        assertThat(smsSenders("it")).hasSize(1);
    }

    @Test
    @DisplayName("未声明 profile → 不注册（新环境 fail-fast）")
    void unknownProfileRegistersNothing() {
        assertThat(smsSenders("staging")).isEmpty();
    }

    private Map<String, SmsSenderPort> smsSenders(String... activeProfiles) {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            if (activeProfiles.length > 0) {
                ctx.getEnvironment().setActiveProfiles(activeProfiles);
            }
            ctx.register(MockSmsSenderAdapter.class);
            ctx.refresh();
            return ctx.getBeansOfType(SmsSenderPort.class);
        }
    }
}
