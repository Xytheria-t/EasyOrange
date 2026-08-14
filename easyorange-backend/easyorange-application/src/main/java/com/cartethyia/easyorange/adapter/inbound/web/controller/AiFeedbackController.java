package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.dto.ChatFeedbackRequest;
import com.cartethyia.easyorange.ai.service.AiFeedbackService;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 输出反馈端点（👍/👎 反馈飞轮）— 前端在回答旁展示两个按钮，点击落 eo_ai_feedback。
 */
@Tag(name = "AI 反馈", description = "AI 输出 👍/👎 反馈（反馈飞轮入口）")
@RestController
@RequestMapping("/api/ai/feedback")
@RequiredArgsConstructor
public class AiFeedbackController {

    private final AiFeedbackService feedbackService;

    @PostMapping
    public Result<Void> feedback(@RequestBody ChatFeedbackRequest request) {
        feedbackService.record(
                request.scope(),
                request.question(),
                request.answer(),
                request.helpful(),
                request.comment(),
                request.callLogId());
        return Result.success();
    }
}
