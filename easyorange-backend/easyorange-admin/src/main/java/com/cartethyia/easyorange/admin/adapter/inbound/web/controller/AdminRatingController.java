package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminRatingDeleteRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminRatingQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminRatingResponse;
import com.cartethyia.easyorange.admin.service.AdminRatingService;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-评价", description = "评价管理")
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminRatingController {

    private final AdminRatingService adminRatingService;

    @GetMapping
    public Result<PageResult<AdminRatingResponse>> listReviews(@Valid AdminRatingQueryRequest request) {
        return Result.success(adminRatingService.listReviews(request));
    }

    @GetMapping("/{id}")
    public Result<AdminRatingResponse> getReviewDetail(@PathVariable String id) {
        return Result.success(adminRatingService.getReviewDetail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteReview(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable String id,
            @Valid @RequestBody AdminRatingDeleteRequest request) {
        adminRatingService.deleteReview(user.userId(), id, request);
        return Result.success();
    }
}
