package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 模型路由 — 按场景选择模型 bean（简单任务走快模型 / 复杂任务走强模型）。
 * <p>
 * 场景 → bean 名的映射在 {@code easyorange.ai.routing.scenarios}（yaml 可热更新），
 * 未配置的场景回退 {@code easyorange.ai.routing.default-model}（默认 chatModel）。
 * 当前只有一个文本模型时路由恒为 chatModel，接入第二个模型（如 deepseek-reasoner）
 * 只需改配置，代码零改动 — 这是「成本治理」的演进位而非现状承诺。
 */
@Component
@RequiredArgsConstructor
public class AiModelRouter {

    private final AiProperties aiProperties;
    private final ApplicationContext applicationContext;

    public ChatModel choose(String scenario) {
        String beanName = aiProperties
                .getRouting()
                .getScenarios()
                .getOrDefault(scenario, aiProperties.getRouting().getDefaultModel());
        try {
            return applicationContext.getBean(beanName, ChatModel.class);
        } catch (BeansException e) {
            return applicationContext.getBean(ChatModel.class);
        }
    }
}
