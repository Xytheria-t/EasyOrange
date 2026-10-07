package com.cartethyia.easyorange.ai.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.application.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.application.listing.AutoListingAppService;
import com.cartethyia.easyorange.ai.domain.enums.AiResultCode;
import com.cartethyia.easyorange.ai.domain.exception.ChatStreamAbortedException;
import com.cartethyia.easyorange.ai.domain.model.ChatSource;
import com.cartethyia.easyorange.ai.domain.model.ToolCallStepView;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.common.annotation.SkipRateLimit;
import com.cartethyia.easyorange.common.annotation.SkipRepeatSubmit;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 上架辅助端点 — 拍照识别（发布助手，多步工具循环）。
 * <p>
 * 两个入口跑同一条链路（视觉预识别 → 工具循环 → 生成表单）：非流式返回 {@code Result} 信封；流式（SSE）
 * 逐步推 step 事件（工具步可视化：查禁售 / 召回同类 / 行情统计），done 带表单 JSON，无 token / sources
 * 事件——发布产出是结构化表单不是散文。
 * <p>
 * 图片张数上限与商品创建侧对齐（9 张）：识别一批图却只能发布 9 张，第 10 张之后纯属白花 token。
 */
@Slf4j
@SkipRateLimit
@Tag(name = "AI 发布助手", description = "卖家发布助手：拍照识别单入口（视觉预识别 + 多步工具循环生成上架表单）")
@Validated
@RestController
@RequestMapping("/api/ai")
public class AiListingController {

    private static final long STREAM_TIMEOUT_MS = 120_000;

    private final AutoListingAppService autoListingService;
    private final ObjectProvider<TaskExecutor> taskExecutors;

    public AiListingController(
            AutoListingAppService autoListingService,
            @Qualifier("applicationTaskExecutor") ObjectProvider<TaskExecutor> taskExecutors) {
        this.autoListingService = autoListingService;
        this.taskExecutors = taskExecutors;
    }

    // 只读识别调用，无状态可重复提交风险；防重 3s 会把「再次识别」误拦成 429（连点/连跑不一致），
    // 频控由 AI 限流（auto-listing 5 次/分）与 token 预算承担
    @SkipRepeatSubmit
    @PostMapping("/auto-listing")
    public Result<AutoListingResult> autoListing(
            @RequestBody @NotEmpty(message = "请至少上传一张图片") @Size(max = 9, message = "图片数量不能超过 9 张")
                    List<@NotBlank(message = "图片地址不能为空") String> imageUrls) {
        String userId = SecurityContextUtil.getUserContextOrThrow().userId();
        return Result.success(autoListingService.analyzeImages(imageUrls, userId, null));
    }

    /** 流式识别 — 事件协议 step / done / error；身份在 servlet 线程解析后显式带进流式线程（与对话流式同型）。 */
    @SkipRepeatSubmit
    @PostMapping("/auto-listing/stream")
    public SseEmitter stream(
            @RequestBody @NotEmpty(message = "请至少上传一张图片") @Size(max = 9, message = "图片数量不能超过 9 张")
                    List<@NotBlank(message = "图片地址不能为空") String> imageUrls) {
        String userId = SecurityContextUtil.getUserContextOrThrow().userId();
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emitter.onTimeout(() -> AiChatController.completeQuietly(emitter));
        emitter.onError(e -> AiChatController.completeQuietly(emitter));
        Runnable task = () -> runStream(emitter, imageUrls, userId);
        var executor = taskExecutors.getIfAvailable();
        if (executor == null) {
            task.run();
        } else {
            executor.execute(task);
        }
        return emitter;
    }

    /** 预算异常在切面（代理边界）抛出，进不了方法体，由这里转 error 事件而不是 500。 */
    private void runStream(SseEmitter emitter, List<String> imageUrls, String userId) {
        try {
            AutoListingResult result = autoListingService.analyzeImages(imageUrls, userId, listingHandler(emitter));
            AiChatController.send(emitter, SseEmitter.event().name("done").data(result));
            AiChatController.completeQuietly(emitter);
        } catch (ChatStreamAbortedException e) {
            // 客户端中途离开（步骤推送时断开）— 静默收尾，不当作服务故障
            AiChatController.completeQuietly(emitter);
        } catch (BaseBusinessException e) {
            // B8001（日预算尽）与 B8002（识别失败）都带面向用户的文案
            sendError(emitter, e.getMessage());
        } catch (Exception e) {
            log.error("action=auto_listing_stream_failed, images={}", imageUrls.size(), e);
            sendError(emitter, AiResultCode.AI_UNAVAILABLE.getMessage());
        }
    }

    /** 步骤事件适配 — token / sources 不是发布链路的事件（产出是表单 JSON），空实现即协议说明。 */
    private ChatStreamHandler listingHandler(SseEmitter emitter) {
        return new ChatStreamHandler() {
            @Override
            public void onStep(ToolCallStepView step) {
                AiChatController.send(emitter, SseEmitter.event().name("step").data(step));
            }

            @Override
            public void onToken(String token) {}

            @Override
            public void onSources(List<ChatSource> sources) {}

            @Override
            public void onDone(String fullAnswer) {}

            @Override
            public void onError(String message) {
                sendError(emitter, message);
            }
        };
    }

    private static void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
        } catch (Exception ignored) {
            // 客户端已断开 / emitter 已完成
        }
        AiChatController.completeQuietly(emitter);
    }
}
