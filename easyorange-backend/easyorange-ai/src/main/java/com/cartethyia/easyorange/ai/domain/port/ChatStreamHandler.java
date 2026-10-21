package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.exception.ChatStreamAbortedException;
import com.cartethyia.easyorange.ai.domain.model.ChatSource;
import com.cartethyia.easyorange.ai.domain.model.ToolCallStepView;
import com.cartethyia.easyorange.ai.domain.model.ToolLoopOutcomeView;
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

    /**
     * 工具调用循环的结局（正常收敛或三条降级之一），生成开始前推送一次。
     * <p>
     * 与降级率指标同源——都从 {@code ToolLoopKernel.Result} 读，不各自判定：界面说收敛、指标说降级这种
     * 对不上，会先骗过读它的人。降级收口仍产出一份正常回答，界面上这是唯一能看出「它是降级产物」的地方。
     */
    void onLoopOutcome(ToolLoopOutcomeView outcome);

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
