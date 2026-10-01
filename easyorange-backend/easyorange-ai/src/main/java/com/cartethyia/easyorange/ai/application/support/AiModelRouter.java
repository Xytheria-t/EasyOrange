package com.cartethyia.easyorange.ai.application.support;

import com.cartethyia.easyorange.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 模型路由 — 按场景选择模型 bean（快模型做工具决策 / 文本模型做生成与评审 / 视觉模型做图片分析）。
 * <p>
 * 场景 → bean 名的映射在 {@code easyorange.ai.routing.scenarios}（yaml 可热更新），未配置的场景回退
 * {@code easyorange.ai.routing.default-model}。决策与生成分属两个场景，是「决策 / 生成分离配快模型」
 * 这条成本治理的落点；接入新模型只改配置，代码零改动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelRouter {

    /** 路由场景键 —— 与 application.yaml {@code easyorange.ai.routing.scenarios} 的键契约对应；场景名只在这里定义，消费方禁再写字面量。 */
    public static final String SCENARIO_CHAT_TOOL = "chat_tool";

    public static final String SCENARIO_VISION = "vision";
    public static final String SCENARIO_JUDGE = "judge";

    private final AiProperties aiProperties;
    private final ApplicationContext applicationContext;

    public ChatModel choose(String scenario) {
        String beanName = aiProperties
                .routing()
                .scenarios()
                .getOrDefault(scenario, aiProperties.routing().defaultModel());
        // 场景键缺失回 default-model 是设计内路径；键配了但 bean 名解析不到是配置错误 —— 静默回退 primary
        // 会把「vision 配错」伪装成「图片分析走文本模型」，fail-fast 让错误显式暴露
        try {
            return applicationContext.getBean(beanName, ChatModel.class);
        } catch (BeansException e) {
            throw new IllegalStateException("模型路由配置错误: scenario=%s -> bean=%s".formatted(scenario, beanName), e);
        }
    }
}
