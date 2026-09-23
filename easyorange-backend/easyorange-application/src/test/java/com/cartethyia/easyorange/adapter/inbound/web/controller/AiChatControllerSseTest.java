package com.cartethyia.easyorange.adapter.inbound.web.controller;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 发送/收尾对「emitter 已完成」的容错 — TD-022。
 * <p>
 * playground 中途刷新/连发时，流线程会对已完成的 emitter 继续写入，
 * {@code ResponseBodyEmitter has already completed}（IllegalStateException）曾作为 ERROR 逃逸；
 * 现收敛为 {@link AiChatController.ClientDisconnectedException} 终止流并降 debug。
 */
@DisplayName("AiChatController SSE 完成态容错")
class AiChatControllerSseTest {

    @Test
    @DisplayName("emitter 已完成后再发事件 → 收敛为 ClientDisconnected，不抛 IllegalStateException")
    void send_afterComplete_convertsToClientDisconnected() {
        SseEmitter emitter = new SseEmitter(1000L);
        emitter.complete();

        assertThatThrownBy(() -> AiChatController.send(emitter, SseEmitter.event().name("token").data("x")))
                .isInstanceOf(AiChatController.ClientDisconnectedException.class);
    }

    @Test
    @DisplayName("对已完成 emitter 二次 complete 不抛（onDone/onError/异常收尾共用路径）")
    void completeQuietly_afterComplete_doesNotThrow() {
        SseEmitter emitter = new SseEmitter(1000L);
        emitter.complete();

        assertThatNoException().isThrownBy(() -> AiChatController.completeQuietly(emitter));
    }

    @Test
    @DisplayName("未完成 emitter：发送与完成均正常")
    void sendAndComplete_beforeCompletion_works() {
        SseEmitter emitter = new SseEmitter(1000L);

        assertThatNoException().isThrownBy(() -> AiChatController.send(emitter, SseEmitter.event().name("token").data("x")));
        assertThatNoException().isThrownBy(() -> AiChatController.completeQuietly(emitter));
    }
}
