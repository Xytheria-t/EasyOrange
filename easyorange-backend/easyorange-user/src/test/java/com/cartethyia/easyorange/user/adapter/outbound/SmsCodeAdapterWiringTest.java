package com.cartethyia.easyorange.user.adapter.outbound;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.user.adapter.outbound.cache.RedisSmsCodeAdapter;
import com.cartethyia.easyorange.user.adapter.outbound.mock.MockSmsCodeAdapter;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 短信验证码适配器装配互斥测试 — 守两类回归：
 * <ol>
 *   <li>两实现同时注册：原 {@code @ConditionalOnMissingBean} 依赖组件扫描顺序，顺序反转即
 *       {@code NoUniqueBeanDefinitionException} 启动失败；</li>
 *   <li>条件永假导致类不可达：文档声称 dev 走内存实现，实际注册的却是 Redis 适配器
 *       （文档与代码漂移，dev 行为与叙事不符）。</li>
 * </ol>
 * 未声明 profile 时不注册任何实现：新环境 fail-fast，防止裸跑容器把 mock 带上生产。
 */
class SmsCodeAdapterWiringTest {

    @Test
    @DisplayName("dev → 只注册内存 MockSmsCodeAdapter")
    void devUsesMemoryAdapter() {
        Map<String, SmsCodePort> beans = smsCodePorts("dev");
        assertThat(beans).hasSize(1);
        assertThat(beans.get("mockSmsCodeAdapter")).isInstanceOf(MockSmsCodeAdapter.class);
    }

    @Test
    @DisplayName("test → 只注册内存 MockSmsCodeAdapter")
    void testUsesMemoryAdapter() {
        Map<String, SmsCodePort> beans = smsCodePorts("test");
        assertThat(beans).hasSize(1);
        assertThat(beans.get("mockSmsCodeAdapter")).isInstanceOf(MockSmsCodeAdapter.class);
    }

    @Test
    @DisplayName("it → 只注册 RedisSmsCodeAdapter（集成测试走真实存储）")
    void itUsesRedisAdapter() {
        Map<String, SmsCodePort> beans = smsCodePorts("it");
        assertThat(beans).hasSize(1);
        assertThat(beans.get("redisSmsCodeAdapter")).isInstanceOf(RedisSmsCodeAdapter.class);
    }

    @Test
    @DisplayName("prod → 只注册 RedisSmsCodeAdapter（内存实现禁止上生产）")
    void prodUsesRedisAdapter() {
        Map<String, SmsCodePort> beans = smsCodePorts("prod");
        assertThat(beans).hasSize(1);
        assertThat(beans.get("redisSmsCodeAdapter")).isInstanceOf(RedisSmsCodeAdapter.class);
    }

    @Test
    @DisplayName("未声明 profile → 不注册任何实现（新环境 fail-fast）")
    void unknownProfileRegistersNothing() {
        assertThat(smsCodePorts()).isEmpty();
    }

    /** 构造与生产同构的最小上下文：两个适配器 + 各自依赖的桩 Bean。 */
    private Map<String, SmsCodePort> smsCodePorts(String... activeProfiles) {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            if (activeProfiles.length > 0) {
                ctx.getEnvironment().setActiveProfiles(activeProfiles);
            }
            ctx.getBeanFactory().registerSingleton("smsSenderPort", (SmsSenderPort) (phone, code) -> {});
            ctx.getBeanFactory().registerSingleton("redisTemplate", new RedisTemplate<Object, Object>());
            ctx.register(MockSmsCodeAdapter.class, RedisSmsCodeAdapter.class);
            ctx.refresh();
            return ctx.getBeansOfType(SmsCodePort.class);
        }
    }
}
