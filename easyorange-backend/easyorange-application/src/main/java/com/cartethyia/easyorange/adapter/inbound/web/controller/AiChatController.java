package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.chat.ChatStreamHandler;
import com.cartethyia.easyorange.ai.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.dto.ChatRequest;
import com.cartethyia.easyorange.ai.service.AiChatService;
import com.cartethyia.easyorange.common.annotation.SkipRateLimit;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话端点 — 非流式（语义缓存 + Judge 回归同源）与 SSE 流式（打字机效果）。
 * <p>
 * SSE 走 POST + SseEmitter（前端用 fetch + ReadableStream 消费，可带 Authorization 头）；
 * 事件协议：token（逐字）/ sources（知识库来源）/ done（完整回答）/ error（降级文案）。
 * 流式工作在虚拟线程上执行，Controller 只负责事件 → SseEmitter 的适配。
 */
@SkipRateLimit
@Tag(name = "AI 对话", description = "多轮 Agent 对话（SSE 流式 + 知识库引用溯源）")
@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private static final long STREAM_TIMEOUT_MS = 120_000;

    private final AiChatService chatService;

    @PostMapping
    public Result<ChatAnswer> chat(@RequestBody ChatRequest request) {
        return Result.success(chatService.answer(request));
    }

    @PostMapping("/stream")
    public SseEmitter stream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        Thread.ofVirtual().name("ai-chat-stream").start(() -> runStream(emitter, request));
        return emitter;
    }

    private void runStream(SseEmitter emitter, ChatRequest request) {
        try {
            chatService.streamAnswer(request, new ChatStreamHandler() {
                @Override
                public void onToken(String token) {
                    send(emitter, SseEmitter.event().name("token").data(token));
                }

                @Override
                public void onSources(List<String> sources) {
                    send(emitter, SseEmitter.event().name("sources").data(sources));
                }

                @Override
                public void onDone(String fullAnswer) {
                    send(emitter, SseEmitter.event().name("done").data(fullAnswer));
                    emitter.complete();
                }

                @Override
                public void onError(String message) {
                    send(emitter, SseEmitter.event().name("error").data(message));
                    emitter.complete();
                }
            });
        } catch (Exception e) {
            // streamAnswer 内部已兜底业务异常，此处仅处理适配层异常（emitter 已关闭等）
            try {
                emitter.send(SseEmitter.event().name("error").data("AI 服务暂时不可用，请稍后重试"));
            } catch (IOException ignored) {
                // 客户端已断开
            }
            emitter.complete();
        }
    }

    private static void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException e) {
            throw new IllegalStateException("SSE client disconnected", e);
        }
    }
}
