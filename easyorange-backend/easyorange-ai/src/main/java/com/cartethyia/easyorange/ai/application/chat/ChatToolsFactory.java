package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.retrieval.AssetSourcingAppService;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalAppService;
import com.cartethyia.easyorange.ai.domain.port.AssetDetailPort;
import com.cartethyia.easyorange.ai.domain.port.UserPreferenceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * 工具面装配 —— 一次循环用的 {@link ChatTools} 从这里实例化（调用方是 {@code ChatToolLoop.run}），编排器不持有
 * 工具面背后的四个协作方（为一次 {@code new} 承担依赖清单，会让工具面演进与循环控制被迫同批）。
 * <p>
 * 只组装不加工：归属用户由调用方判定后传入（null = 机器主体）；每次循环一份新实例是 {@link ChatTools}
 * 自己的并发要求（召回累加器是实例独占的可变状态）。
 */
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChatToolsFactory {

    private final KnowledgeRetrievalAppService retrievalService;
    private final AssetSourcingAppService assetSourcingService;
    private final AssetDetailPort assetDetailPort;
    private final UserPreferenceRepository preferenceRepository;

    ChatTools create(@Nullable String attributedUserId) {
        return new ChatTools(
                retrievalService, assetSourcingService, assetDetailPort, preferenceRepository, attributedUserId);
    }
}
