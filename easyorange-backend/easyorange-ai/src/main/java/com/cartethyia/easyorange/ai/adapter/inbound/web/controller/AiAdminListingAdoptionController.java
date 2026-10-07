package com.cartethyia.easyorange.ai.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.adapter.inbound.web.assembler.AiListingAdoptionAssembler;
import com.cartethyia.easyorange.ai.adapter.inbound.web.dto.response.AiListingAdoptionVO;
import com.cartethyia.easyorange.ai.application.support.AiListingAdoptionAppService;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 建议采纳率（管理端）— 逐字段采纳率与价格偏离分布，使用侧效果数字、不依赖 LLM 判分。
 * /api/admin/** 由 SecurityConfig 统一限 ADMIN 角色。
 */
@Tag(name = "AI 报表", description = "AI 建议字段级采纳率（使用侧效果数字）")
@RestController
@RequestMapping("/api/admin/ai/listing-adoption")
@RequiredArgsConstructor
public class AiAdminListingAdoptionController {

    private final AiListingAdoptionAppService adoptionService;

    @GetMapping
    public Result<AiListingAdoptionVO> listingAdoption() {
        return Result.success(AiListingAdoptionAssembler.toVO(adoptionService.report()));
    }
}
