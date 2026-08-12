package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.CreateRatingRequest;
import com.cartethyia.easyorange.product.application.command.ProductRatingCommandHandler;
import com.cartethyia.easyorange.product.application.query.ProductRatingQueryHandler;
import com.cartethyia.easyorange.product.application.query.dto.ProductRatingVO;
import com.cartethyia.easyorange.product.application.query.dto.RatingStatsVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商品管理", description = "商品评价")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductRatingController {

    private final ProductRatingQueryHandler reviewQueryHandler;
    private final ProductRatingCommandHandler reviewCommandHandler;

    @GetMapping("/{productId}/reviews")
    public Result<PageResult<ProductRatingVO>> listReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(reviewQueryHandler.listReviews(productId, pageNum, pageSize));
    }

    @GetMapping("/{productId}/reviews/stats")
    public Result<RatingStatsVO> getReviewStats(@PathVariable String productId) {
        return Result.success(reviewQueryHandler.getReviewStats(productId));
    }

    @PostMapping("/{productId}/reviews")
    public Result<String> createReview(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable String productId,
            @Valid @RequestBody CreateRatingRequest request) {
        return Result.success(reviewCommandHandler.createReview(user.userId(), request.toCommand(productId)));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public Result<Void> deleteReview(@AuthenticationPrincipal AuthUser user, @PathVariable String reviewId) {
        reviewCommandHandler.deleteReview(user.userId(), reviewId);
        return Result.success();
    }

    @PostMapping("/reviews/{reviewId}/like")
    public Result<Void> likeReview(@PathVariable String reviewId) {
        reviewCommandHandler.likeReview(reviewId);
        return Result.success();
    }
}
