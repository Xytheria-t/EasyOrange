package com.cartethyia.easyorange.ai.application.support;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.enums.ModelCallScope;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 模型路由 —— 按<b>模型角色</b>选模型 bean（decision 决策 / vision 视觉 / judge 评审），角色 → bean 名映射在
 * {@code easyorange.ai.routing.roles}（yaml 可热更新），未配置的角色回退 {@code default-model}；接入新模型只改配置。
 * <p>
 * 在链路里的位置：{@link ModelCaller} 解析角色化调用时唯一的协作者 —— 调用方声明角色键，由本类挑出 bean 交回；
 * 本类不发起调用、不记账。
 * <p>
 * 与 {@link ModelCallScope} 是两条正交轴，别混：本类答「这次调用用哪个模型」（按任务性质），scope 答「这笔钱记谁头上」
 * （按业务链路）。同一次用户请求会跨多个模型角色（决策用快模型、生成用强模型），但只落一个业务链路的账。
 */
@Component
@RequiredArgsConstructor
public class ModelRouter {

    /** 路由角色键 — 与 yaml {@code easyorange.ai.routing.roles} 的键契约对应，消费方引用常量不写字面量。 */
    public static final String ROLE_DECISION = "decision";

    public static final String ROLE_VISION = "vision";
    public static final String ROLE_JUDGE = "judge";

    private final AiProperties aiProperties;
    private final ApplicationContext applicationContext;

    /** 取该角色对应的模型 bean。 */
    public ChatModel chooseForRole(String role) {
        String beanName = aiProperties
                .routing()
                .roles()
                .getOrDefault(role, aiProperties.routing().defaultModel());
        // 角色键缺失回 default-model 是设计内路径；键配了但 bean 解析不到是配置错误 —— 静默回退 primary
        // 会把「vision 配错」伪装成「图片分析走文本模型」，fail-fast 让错误显式暴露
        try {
            return applicationContext.getBean(beanName, ChatModel.class);
        } catch (BeansException e) {
            throw new IllegalStateException("模型路由配置错误: role=%s -> bean=%s".formatted(role, beanName), e);
        }
    }
}
