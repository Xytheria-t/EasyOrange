package com.cartethyia.easyorange.ai.testsupport;

import static org.mockito.Mockito.mock;

import com.cartethyia.easyorange.ai.adapter.outbound.budget.InMemoryTokenBudgetStore;
import com.cartethyia.easyorange.ai.application.support.AiCallRecorder;
import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.port.AiCallLogPort;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStore;
import tools.jackson.databind.ObjectMapper;

/**
 * AiModelSupport 测试夹具 — 依赖装配收在一处。
 * <p>
 * 记账已抽成 {@link AiCallRecorder}，此处只负责把「日志端口 + 预算存储 + 配置」拼成 recorder 再塞进
 * support：配置要同时喂给两边（recorder 记账判场景上限，support 下发 max_tokens），所以出现两次
 * 是真实的依赖关系，不是漏改。
 * <p>
 * 构造器每加一个横切依赖，散在各测试里的 {@code new} 就要跟着改；集中后只改这里。
 * 需要断言记账行为的用例走三参重载，自己传 {@link InMemoryTokenBudgetStore}。
 */
public final class TestAiModelSupport {

    private TestAiModelSupport() {}

    /** 全 mock/默认装配：调用日志不落库、预算用内存实现、配置全默认值。 */
    public static AiModelSupport create() {
        return create(mock(AiCallLogPort.class));
    }

    public static AiModelSupport create(AiCallLogPort callLogPort) {
        return create(callLogPort, new InMemoryTokenBudgetStore(), PropertyBindings.bind(AiProperties.class));
    }

    public static AiModelSupport create(
            AiCallLogPort callLogPort, TokenBudgetStore budgetStore, AiProperties aiProperties) {
        return new AiModelSupport(
                new AiCallRecorder(callLogPort, budgetStore, aiProperties), aiProperties, new ObjectMapper());
    }
}
