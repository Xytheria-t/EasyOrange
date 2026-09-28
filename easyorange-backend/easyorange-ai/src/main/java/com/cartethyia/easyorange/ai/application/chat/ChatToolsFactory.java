package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.retrieval.AssetSourcingAppService;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalAppService;
import com.cartethyia.easyorange.ai.domain.port.AssetDetailPort;
import com.cartethyia.easyorange.ai.domain.port.UserPreferenceRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * 工具面装配 — 一次循环用的 {@link ChatTools} 从这里实例化，编排器不持有工具面背后的四个协作方。
 * <p>
 * 取舍：这四个依赖（规则检索 / 在售资产检索 / 资产详情 / 偏好写入）在编排器里各自只被消费一次，
 * 唯一的去处是 {@code new ChatTools(...)}。让编排器为一次 new 承担四个依赖，等于把「工具面由谁组装」
 * 焊死在循环控制里 —— 加一个工具的协作方就要改循环的构造器，工具面的演进与循环控制的演进被迫同批。
 * <p>
 * 边界：只组装不加工。归属用户由调用方判定后传入（机器主体返回 null 的口径留在编排器，
 * 因为同一口径还管 trace 的 user_id，拆开会出现两处各判一次的漂移面）；本类不做身份与上下文判断，
 * 也不决定实例的生命周期之外的任何事 —— 每次循环一份实例是 {@link ChatTools} 自己的并发要求
 * （召回累加器是实例独占的可变状态），不是这里的策略。
 */
@Component
public class ChatToolsFactory {

    private final KnowledgeRetrievalAppService retrievalService;
    private final AssetSourcingAppService assetSourcingService;
    private final AssetDetailPort assetDetailPort;
    private final UserPreferenceRepository preferenceRepository;

    ChatToolsFactory(
            KnowledgeRetrievalAppService retrievalService,
            AssetSourcingAppService assetSourcingService,
            AssetDetailPort assetDetailPort,
            UserPreferenceRepository preferenceRepository) {
        this.retrievalService = retrievalService;
        this.assetSourcingService = assetSourcingService;
        this.assetDetailPort = assetDetailPort;
        this.preferenceRepository = preferenceRepository;
    }

    ChatTools create(@Nullable String attributedUserId) {
        return new ChatTools(
                retrievalService, assetSourcingService, assetDetailPort, preferenceRepository, attributedUserId);
    }
}
