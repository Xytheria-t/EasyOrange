package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.CreateRatingRequest;
import com.cartethyia.easyorange.product.application.command.ProductRatingCommandService;
import com.cartethyia.easyorange.product.application.query.ProductRatingQueryService;
import com.cartethyia.easyorange.product.application.query.dto.ProductRatingVO;
import com.cartethyia.easyorange.product.application.query.dto.RatingStatsVO;
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

    private final ProductRatingQueryService reviewQueryService;
    private final ProductRatingCommandService reviewCommandService;

    @GetMapping("/{productId}/reviews")
    public Result<PageResult<ProductRatingVO>> listReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(reviewQueryService.listReviews(productId, pageNum, pageSize));
    }

    @GetMapping("/{productId}/reviews/stats")
    public Result<RatingStatsVO> getReviewStats(@PathVariable String productId) {
        return Result.success(reviewQueryService.getReviewStats(productId));
    }

    @PostMapping("/{productId}/reviews")
    public Result<String> createReview(
            @PathVariable String productId,
            @Valid @RequestBody CreateRatingRequest request) {
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
