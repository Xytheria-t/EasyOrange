package com.cartethyia.easyorange.ai.application.support;

import com.cartethyia.easyorange.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 模型路由 — 按场景选择模型 bean（快模型做工具决策 / 文本模型做生成与评审 / 视觉模型做图片分析）。
 * <p>
 * 场景 → bean 名的映射在 {@code easyorange.ai.routing.scenarios}（yaml 可热更新，场景清单以 application.yaml
 * 为准），未配置的场景回退 {@code easyorange.ai.routing.default-model}。决策与生成分属两个场景，是
 * 「决策 / 生成分离配快模型」这条成本治理的落点；接入新模型只改配置，代码零改动。
 */
@Component
@RequiredArgsConstructor
public class AiModelRouter {

    private final AiProperties aiProperties;
    private final ApplicationContext applicationContext;

    public ChatModel choose(String scenario) {
        String beanName = aiProperties
                .routing()
                .scenarios()
                .getOrDefault(scenario, aiProperties.routing().defaultModel());
        try {
            return applicationContext.getBean(beanName, ChatModel.class);
        } catch (BeansException e) {
            return applicationContext.getBean(ChatModel.class);
        }
    }
}
