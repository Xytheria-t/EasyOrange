package com.cartethyia.easyorange.ai.application.dto;

import com.cartethyia.easyorange.ai.domain.model.ChatSource;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * AI 对话回答 — 带引用溯源：来源结构化（类型 + id，非纯标题），前端据此把商品引用渲染成可点卡片、
 * 规则引用渲染成可展开引文（见 {@link ChatSource}）；正文以 [来源:标题] 标注。
 *
 * @param degraded 非模型实时生成（供应商故障复用 stale 旧回答，或无旧回答可兜底的降级文案）；降级必须对
 *                 调用方与埋点可见，否则「AI 挂了」会被统计成「AI 答得差」。
 */
public record ChatAnswer(
        String answer, List<ChatSource> sources, @Nullable String sessionId, boolean degraded) {

    /** 供应商故障且无旧回答可兜底时的统一文案（非流式与 SSE error 事件同源，避免两处口径漂移）。 */
    public static final String UNAVAILABLE_TEXT = "AI 服务暂时不可用，请稍后重试";

    /** 降级回答：模型不可用且没有旧回答可复用。 */
    public static ChatAnswer unavailable(@Nullable String sessionId) {
        return new ChatAnswer(UNAVAILABLE_TEXT, List.of(), sessionId, true);
    }

    /** 标记为降级 —— stale 兜底复用旧回答，非本次实时生成。 */
    public ChatAnswer asDegraded() {
        return degraded ? this : new ChatAnswer(answer, sources, sessionId, true);
    }

    /**
     * 换成当前请求的会话 id —— sessionId 是请求上下文而非回答内容，但两个缓存存的是整个 {@code ChatAnswer}，
     * 复用旧回答会把首个请求的 id 一并带出（同一问题换会话再问会拿到旧会话 id）；缓存命中处一律改成当前
     * 请求的 id，响应语义才是「这次请求的回答」。
     */
    public ChatAnswer withSessionId(@Nullable String sessionId) {
        return Objects.equals(sessionId, this.sessionId) ? this : new ChatAnswer(answer, sources, sessionId, degraded);
    }
}
