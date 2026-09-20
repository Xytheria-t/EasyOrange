package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.application.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.application.service.AutoListingService;
import com.cartethyia.easyorange.common.annotation.SkipRateLimit;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 上架辅助端点 — 拍照识别（Vision）。
 * <p>
 * 只留一个入口：识别结果已经填满上架表单要的字段（属性 / 建议价 / 标题 / 描述）——
 * 独立的估值 / 文案 / 审核建议入口是对同一批产出的重复调用，勿再加回。
 * <p>
 * 图片张数上限与商品创建侧对齐（9 张）：识别一批图却只能发布 9 张，第 10 张之后纯属白花 token。
 */
@SkipRateLimit
@Tag(name = "AI 服务", description = "AI 上架辅助：拍照识别")
@Validated
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiListingController {

    private final AutoListingService autoListingService;

    @PostMapping("/auto-listing")
    public Result<AutoListingResult> autoListing(
            @RequestBody @NotEmpty(message = "请至少上传一张图片") @Size(max = 9, message = "图片数量不能超过 9 张")
                    List<@NotBlank(message = "图片地址不能为空") String> imageUrls) {
        return Result.success(autoListingService.analyzeImages(imageUrls));
    }
}
