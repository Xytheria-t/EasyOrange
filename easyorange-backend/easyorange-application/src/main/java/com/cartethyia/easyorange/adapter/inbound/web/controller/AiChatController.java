package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.application.chat.AiChatService;
import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.application.dto.ChatRequest;
import com.cartethyia.easyorange.ai.domain.model.AgentStepView;
import com.cartethyia.easyorange.ai.domain.model.ChatSource;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamAbortedException;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.common.annotation.SkipRateLimit;
import com.cartethyia.easyorange.common.annotation.SkipRepeatSubmit;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话端点 — 非流式（语义缓存 + Judge 回归同源）与 SSE 流式（打字机效果）。
 * <p>
 * SSE 走 POST + SseEmitter（前端用 fetch + ReadableStream 消费，可带 Authorization 头）；
 * 事件协议：step（Agent 工具循环每步：工具 + 决策理由 + 观察摘要，前端步骤可视化）/
 * token（逐字）/ sources（知识库来源）/ done（完整回答）/ error（降级文案）。
 * 流式工作提交到 {@code applicationTaskExecutor}（spring.threads.virtual.enabled=true，虚拟线程），
 * 而非裸 {@code Thread.ofVirtual()}：后者不继承任何 ThreadLocal，SecurityContext / Observation / MDC
 * 全部丢失，长期用户画像与 Langfuse 父 trace 会在唯一的流式路径上静默失效。
 * {@link SkipRepeatSubmit} 豁免防重：对话非写操作，同一问题 3 秒内二次提交是合法动作
 * （流式卡住时用户停掉立刻重试是最常见的路径），误拦会以 HTTP 429 打断重试。
 * Controller 只负责事件 → SseEmitter 的适配；客户端断开视为正常收尾，不补发 error。
 */
@Slf4j
@SkipRateLimit
@SkipRepeatSubmit
@Tag(name = "AI 对话", description = "多轮 Agent 对话（SSE 流式 + 知识库引用溯源）")
@RestController
@RequestMapping("/api/ai/chat")
public class AiChatController {

    private static final long STREAM_TIMEOUT_MS = 120_000;

    private final AiChatService chatService;
    private final ObjectProvider<TaskExecutor> taskExecutors;

    public AiChatController(
            AiChatService chatService,
            @Qualifier("applicationTaskExecutor") ObjectProvider<TaskExecutor> taskExecutors) {
        this.chatService = chatService;
        this.taskExecutors = taskExecutors;
    }

    @PostMapping
    public Result<ChatAnswer> chat(@Valid @RequestBody ChatRequest request) {
        return Result.success(chatService.answer(request));
    }

    @PostMapping("/stream")
    public SseEmitter stream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        // 客户端挂到超时 / 传输错误 = 客户端侧放弃（刷新、关页、代理掐线）：注册回调安静收尾，
        // 否则超时走 AsyncRequestTimeoutException 落 GlobalExceptionHandler 的 system_error ERROR 兜底——
        // 把「客户端走了」误记成服务故障。onTimeout 里 complete 后容器不再抛超时异常
        emitter.onTimeout(() -> completeQuietly(emitter));
        emitter.onError(e -> {
            log.debug("sse emitter error, client gone", e);
            completeQuietly(emitter);
        });
        // 登录身份在 servlet 线程上取（此时 SecurityContext 还在），显式带进流式线程：
        // 即便执行器没有传播上下文，画像与 trace 的 userId 也不会退化成 anonymous
        Optional<AuthUser> authUser = SecurityContextUtil.getUserContext();
        Runnable task = () -> runStream(emitter, request, authUser.orElse(null));
        var executor = taskExecutors.getIfAvailable();
        // 取不到执行器（极简上下文/手工构造）才退回当前线程 —— 正式装配下绝不能走这条分支：
        // 内联执行会让 emitter 拖到流结束才返回，全部事件积压成一次性回放，SSE 退化成同步接口
        if (executor == null) {
            task.run();
        } else {
            executor.execute(task);
        }
        return emitter;
    }

    private void runStream(SseEmitter emitter, ChatRequest request, @Nullable AuthUser authUser) {
        try {
            chatService.streamAnswer(request, authUser, new ChatStreamHandler() {
                @Override
                public void onStep(AgentStepView step) {
                    send(emitter, SseEmitter.event().name("step").data(step));
                }

                @Override
                public void onToken(String token) {
                    send(emitter, SseEmitter.event().name("token").data(token));
                }

                @Override
                public void onSources(List<ChatSource> sources) {
                    send(emitter, SseEmitter.event().name("sources").data(sources));
                }

                @Override
                public void onDone(String fullAnswer) {
                    send(emitter, SseEmitter.event().name("done").data(fullAnswer));
                    completeQuietly(emitter);
                }

                @Override
                public void onError(String message) {
                    send(emitter, SseEmitter.event().name("error").data(message));
                    completeQuietly(emitter);
                }
            });
        } catch (ChatStreamAbortedException e) {
            // 客户端断开（含 emitter 已完成：中途刷新/连发竞态）— 静默收尾，不当作服务故障补发 error
            completeQuietly(emitter);
        } catch (Exception e) {
            // 适配层兜底（业务异常已由 streamAnswer 内部路由到 onError）
            sendError(emitter);
            completeQuietly(emitter);
        }
    }

    /**
     * 发送单个 SSE 事件 — 客户端断开（IOException）与 emitter 已完成
     * （{@code IllegalStateException: ResponseBodyEmitter has already completed}，TD-022：
     * 中途离开/连发时对已完成 emitter 继续写入）都收敛为 {@link ChatStreamAbortedException} 终止流；
     * 该异常由 ai 侧 {@code streamAnswer} 识别为「客户端离开」静默收尾，不记模型降级。
     */
    static void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException e) {
            throw new ChatStreamAbortedException(e);
        } catch (IllegalStateException e) {
            log.debug("sse emitter already completed, abort stream", e);
            throw new ChatStreamAbortedException(e);
        }
    }

    /** 完成 emitter — 已完成时的二次 complete 同样只降 debug（onDone/onError/异常收尾共用）。 */
    static void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException e) {
            log.debug("sse emitter already completed", e);
        }
    }

    private static void sendError(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("error").data(ChatAnswer.UNAVAILABLE_TEXT));
        } catch (IOException | IllegalStateException ignored) {
            // 客户端已断开 / emitter 已完成
        }
    }
}
