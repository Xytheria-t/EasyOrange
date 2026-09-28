package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.ChatSource;
import com.cartethyia.easyorange.ai.domain.model.ToolCallStepView;
import java.util.List;

/**
 * 流式回答回调 — 服务侧与传输侧（SseEmitter）解耦：
 * 服务只向回调推事件，SSE 适配在 Controller 层完成，便于单测。
 * <p>
 * 回调实现抛 {@link ChatStreamAbortedException} 表示客户端已离开，服务侧按中断静默收尾
 *（不当作模型故障，见该异常的说明）。
 */
public interface ChatStreamHandler {

    /** 工具调用循环的每一步（决策理由 + 观察摘要），生成开始前推送，前端步骤可视化。 */
    void onStep(ToolCallStepView step);

    void onToken(String token);

    /**
     * 引用来源（在生成开始前推送，前端可先渲染来源区）。
     * <p>
     * 带类型与 id 而非纯标题：前端据此把商品引用渲染成可点进商品页的卡片、
     * 规则引用渲染成引文，商品不会再被标成「知识库来源」。
     */
    void onSources(List<ChatSource> sources);

    void onDone(String fullAnswer);

    /** 出错（预算超限 / 模型异常等），携带给用户的兜底文案。 */
    void onError(String message);
}
