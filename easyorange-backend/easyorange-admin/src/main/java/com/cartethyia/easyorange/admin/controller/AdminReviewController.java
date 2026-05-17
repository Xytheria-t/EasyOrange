package com.cartethyia.easyorange.admin.controller;

import com.cartethyia.easyorange.admin.dto.request.AdminReviewDeleteRequest;
import com.cartethyia.easyorange.admin.dto.request.AdminReviewQueryRequest;
import com.cartethyia.easyorange.admin.dto.response.AdminReviewVO;
import com.cartethyia.easyorange.admin.service.AdminReviewService;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @GetMapping
    public Result<PageResult<AdminReviewVO>> listReviews(@Valid AdminReviewQueryRequest request) {
        return Result.success(adminReviewService.listReviews(request));
    }

    @GetMapping("/{id}")
    public Result<AdminReviewVO> getReviewDetail(@PathVariable Long id) {
        return Result.success(adminReviewService.getReviewDetail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteReview(
        @PathVariable Long id,
        @Valid @RequestBody AdminReviewDeleteRequest request
    ) {
        adminReviewService.deleteReview(id, request);
        return Result.success();
    }
}
