package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.CreateReviewRequest;
import com.cartethyia.easyorange.product.application.command.ProductReviewCommandService;
import com.cartethyia.easyorange.product.application.query.ProductReviewQueryService;
import com.cartethyia.easyorange.product.application.query.ProductReviewVO;
import com.cartethyia.easyorange.product.application.query.ReviewStatsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductRatingController {

    private final ProductReviewQueryService reviewQueryService;
    private final ProductReviewCommandService reviewCommandService;

    @GetMapping("/{productId}/reviews")
    public Result<PageResult<ProductReviewVO>> listReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(reviewQueryService.listReviews(productId, pageNum, pageSize));
    }

    @GetMapping("/{productId}/reviews/stats")
    public Result<ReviewStatsVO> getReviewStats(@PathVariable String productId) {
        return Result.success(reviewQueryService.getReviewStats(productId));
    }

    @PostMapping("/{productId}/reviews")
    public Result<String> createReview(
            @PathVariable String productId,
            @Valid @RequestBody CreateReviewRequest request) {
        return Result.success(reviewCommandService.createReview(request.toCommand(productId)));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public Result<Void> deleteReview(@PathVariable String reviewId) {
        reviewCommandService.deleteReview(reviewId);
        return Result.success();
    }

    @PostMapping("/reviews/{reviewId}/like")
    public Result<Void> likeReview(@PathVariable String reviewId) {
        reviewCommandService.likeReview(reviewId);
        return Result.success();
    }
}
