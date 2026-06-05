package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.dto.CreditScoreResult;
import com.cartethyia.easyorange.ai.service.CreditScoringService;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
public class CreditScoreController {

    private final CreditScoringService creditScoringService;

    @GetMapping("/me")
    public Result<CreditScoreResult> getMyCredit() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return Result.success(creditScoringService.getCreditScore(userId));
    }

    @GetMapping("/{userId}")
    public Result<CreditScoreResult> getUserCredit(@PathVariable Long userId) {
        return Result.success(creditScoringService.getCreditScore(userId));
    }

    @PostMapping("/recalculate")
    public Result<Void> recalculateScore() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        creditScoringService.recalculateScore(userId);
        return Result.success();
    }
}