package com.cartethyia.easyorange.ai.application.dto;

import com.cartethyia.easyorange.ai.domain.model.ChatSource;
import java.util.List;
import java.util.Objects;

/**
 * AI 对话回答 — 带引用溯源（结构化来源，回答末尾用 [来源:标题] 标注）。
 * <p>
 * {@code sources} 带类型与 id 而非纯标题：前端据此把商品引用渲染成可点进商品页的卡片、
 * 把规则引用渲染成可展开的引文，两条链各走各的（见 {@link ChatSource}）。
 *
 * @param degraded 本次回答不是模型实时生成的结果（供应商故障时复用 stale 旧回答，或没有旧回答可兜底的降级文案）。
 *                 降级必须对调用方与埋点可见，否则「AI 挂了」会被统计成「AI 答得差」。
 */
public record ChatAnswer(String answer, List<ChatSource> sources, String sessionId, boolean degraded) {

    /** 供应商故障且无旧回答可兜底时的统一文案（非流式与 SSE error 事件同源，避免两处口径漂移）。 */
    public static final String UNAVAILABLE_TEXT = "AI 服务暂时不可用，请稍后重试";

    /** 降级回答：模型不可用且没有旧回答可复用。 */
    public static ChatAnswer unavailable(String sessionId) {
        return new ChatAnswer(UNAVAILABLE_TEXT, List.of(), sessionId, true);
    }

    /** 标记为降级（stale 兜底复用旧回答，但它不是本次实时生成的）。 */
    public ChatAnswer asDegraded() {
        return degraded ? this : new ChatAnswer(answer, sources, sessionId, true);
    }

    /**
     * 换成当前请求的会话 id。
     * <p>
     * {@code sessionId} 是请求上下文而不是回答内容，但两个缓存存的是整个 {@code ChatAnswer}，
     * 复用旧回答时会把第一次那个请求的 id 一起带出来（同一问题换个会话再问，响应里的 id 仍是旧会话的）。
     * 缓存命中处一律用它改成当前请求的 id，响应语义才是「这次请求的回答」。
     */
    public ChatAnswer withSessionId(String sessionId) {
        return Objects.equals(sessionId, this.sessionId) ? this : new ChatAnswer(answer, sources, sessionId, degraded);
    }
}
