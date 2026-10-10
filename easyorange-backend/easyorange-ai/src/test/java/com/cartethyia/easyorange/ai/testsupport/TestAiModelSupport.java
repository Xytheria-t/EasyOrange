package com.cartethyia.easyorange.ai.testsupport;

import static org.mockito.Mockito.mock;

import com.cartethyia.easyorange.ai.adapter.outbound.budget.InMemoryTokenBudgetStore;
import com.cartethyia.easyorange.ai.application.support.AiCallRecorder;
import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.port.AiCallLogPort;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort;
import tools.jackson.databind.ObjectMapper;

/**
 * AiModelSupport 测试夹具 — 依赖装配收在一处：构造器每加一个横切依赖，只改这里，不用改散在各测试的
 * {@code new}。
 * <p>
 * 配置在 recorder（记账判场景上限）与 support（下发 max_tokens）各出现一次，是真实依赖关系，不是漏改。
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
            AiCallLogPort callLogPort, TokenBudgetStorePort budgetStore, AiProperties aiProperties) {
        return new AiModelSupport(
                new AiCallRecorder(callLogPort, budgetStore, aiProperties), aiProperties, new ObjectMapper());
    }
}
