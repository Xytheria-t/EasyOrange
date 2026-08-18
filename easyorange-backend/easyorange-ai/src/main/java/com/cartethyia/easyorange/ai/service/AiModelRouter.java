package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 模型路由 — 按场景选择模型 bean（对话走 DeepSeek 文本模型 / 图片分析走 Qwen-VL 视觉模型）。
 * <p>
 * 场景 → bean 名的映射在 {@code easyorange.ai.routing.scenarios}（yaml 可热更新），
 * 未配置的场景回退 {@code easyorange.ai.routing.default-model}（默认 chatModel）。
 * 已接入场景：chat_tool → chatModel（工具决策）、vision → visionChatModel（图片分析）；
 * 接入新模型只需改配置，代码零改动 — 这是「成本治理」的演进位，新增模型按需接入。
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
