package com.cartethyia.easyorange.ai.chat;

import java.util.List;

/**
 * 流式回答回调 — 服务侧与传输侧（SseEmitter）解耦：
 * 服务只向回调推事件，SSE 适配在 Controller 层完成，便于单测。
 */
public interface ChatStreamHandler {

    /** 生成过程中的每个 token。 */
    void onToken(String token);

    /** 知识库引用来源（在生成开始前推送，前端可先渲染来源区）。 */
    void onSources(List<String> sources);

    /** 流结束，携带完整回答。 */
    void onDone(String fullAnswer);

    /** 出错（预算超限 / 模型异常等），携带给用户的兜底文案。 */
    void onError(String message);
}
