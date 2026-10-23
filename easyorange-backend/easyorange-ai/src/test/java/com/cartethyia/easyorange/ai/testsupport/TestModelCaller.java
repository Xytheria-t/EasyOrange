package com.cartethyia.easyorange.ai.testsupport;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.cartethyia.easyorange.ai.adapter.outbound.budget.InMemoryTokenBudgetStore;
import com.cartethyia.easyorange.ai.application.support.ModelCallRecorder;
import com.cartethyia.easyorange.ai.application.support.ModelCaller;
import com.cartethyia.easyorange.ai.application.support.ModelRouter;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.port.ModelCallLogPort;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort;
import org.springframework.ai.chat.model.ChatModel;
import tools.jackson.databind.ObjectMapper;

/**
 * ModelCaller 测试夹具 — 依赖装配收在一处：构造器每加一个横切依赖，只改这里，不用改散在各测试的
 * {@code new}。
 * <p>
 * 配置在 recorder（记账判场景上限）与 support（下发 max_tokens）各出现一次，是真实依赖关系，不是漏改。
 * 需要断言记账行为的用例走三参重载，自己传 {@link InMemoryTokenBudgetStore}。
 */
public final class TestModelCaller {

    private TestModelCaller() {}

    /** 全 mock/默认装配：调用日志不落库、预算用内存实现、配置全默认值；角色化调用解析到通用 mock 模型。 */
    public static ModelCaller create() {
        return create(mock(ModelCallLogPort.class));
    }

    /** 指定路由 —— 用例要控制「哪个角色解析到哪个模型」时用（默认那条路只解析出通用 mock）。 */
    public static ModelCaller create(ModelRouter modelRouter) {
        return create(
                mock(ModelCallLogPort.class),
                new InMemoryTokenBudgetStore(),
                PropertyBindings.bind(AiProperties.class),
                modelRouter);
    }

    public static ModelCaller create(ModelCallLogPort callLogPort) {
        return create(callLogPort, new InMemoryTokenBudgetStore(), PropertyBindings.bind(AiProperties.class));
    }

    /** 指定调用日志端口 + 路由 —— 用例自己桩模型（{@code call} / {@code stream}）时用。 */
    public static ModelCaller create(ModelCallLogPort callLogPort, ModelRouter modelRouter) {
        return create(
                callLogPort, new InMemoryTokenBudgetStore(), PropertyBindings.bind(AiProperties.class), modelRouter);
    }

    public static ModelCaller create(
            ModelCallLogPort callLogPort, TokenBudgetStorePort budgetStore, AiProperties aiProperties) {
        return create(callLogPort, budgetStore, aiProperties, anyModelRouter());
    }

    public static ModelCaller create(
            ModelCallLogPort callLogPort,
            TokenBudgetStorePort budgetStore,
            AiProperties aiProperties,
            ModelRouter modelRouter) {
        return new ModelCaller(
                new ModelCallRecorder(callLogPort, budgetStore, aiProperties),
                modelRouter,
                aiProperties,
                new ObjectMapper());
    }

    /** 任意角色键都解析到同一个 mock 模型 —— 用例不关心模型选型时的默认路由。 */
    private static ModelRouter anyModelRouter() {
        return routerReturning(mock(ChatModel.class));
    }

    /** 角色键一律解析到给定模型 —— 用例自己桩模型的 {@code call} / {@code stream} 时用它固定路由。 */
    public static ModelRouter routerReturning(ChatModel chatModel) {
        ModelRouter modelRouter = mock(ModelRouter.class);
        lenient().when(modelRouter.chooseForRole(anyString())).thenReturn(chatModel);
        return modelRouter;
    }
}
