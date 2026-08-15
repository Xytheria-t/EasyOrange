package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.dto.CreditScoreResult;
import com.cartethyia.easyorange.ai.service.CreditScoringService;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 服务", description = "用户信用评分")
@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
public class CreditScoreController {

    private final CreditScoringService creditScoringService;

    @GetMapping("/me")
    public Result<CreditScoreResult> getMyCredit(@AuthenticationPrincipal AuthUser user) {
        return Result.success(creditScoringService.getCreditScore(user.userId()));
    }

    @PostMapping("/recalculate")
    public Result<Void> recalculateScore(@AuthenticationPrincipal AuthUser user) {
        creditScoringService.recalculateScore(user.userId());
        return Result.success();
    }
}
