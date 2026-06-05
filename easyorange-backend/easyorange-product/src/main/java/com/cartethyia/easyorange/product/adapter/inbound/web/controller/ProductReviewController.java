package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.CreateReviewRequest;
import com.cartethyia.easyorange.product.application.command.CreateProductReviewCommand;
import com.cartethyia.easyorange.product.application.command.ProductReviewCommandService;
import com.cartethyia.easyorange.product.application.query.ProductReviewQueryService;
import com.cartethyia.easyorange.product.application.query.dto.ProductReviewVO;
import com.cartethyia.easyorange.product.application.query.dto.ReviewStatsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class ProductReviewController {

    private final ProductReviewQueryService reviewQueryService;
    private final ProductReviewCommandService reviewCommandService;

    @GetMapping("/{productId}/reviews")
    public Result<PageResult<ProductReviewVO>> listReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(reviewQueryService.listReviews(productId, pageNum, pageSize));
    }

    @GetMapping("/{productId}/reviews/stats")
    public Result<ReviewStatsVO> getReviewStats(@PathVariable Long productId) {
        return Result.success(reviewQueryService.getReviewStats(productId));
    }

    @PostMapping("/{productId}/reviews")
    public Result<Long> createReview(
            @PathVariable Long productId,
            @Valid @RequestBody CreateReviewRequest request) {
        CreateProductReviewCommand command = CreateProductReviewCommand.builder()
                .productId(productId)
                .rating(request.getRating())
                .content(request.getContent())
                .build();
        return Result.success(reviewCommandService.createReview(command));
    }

    @DeleteMapping("/{productId}/reviews/{reviewId}")
    public Result<Void> deleteReview(@PathVariable Long productId, @PathVariable Long reviewId) {
        reviewCommandService.deleteReview(reviewId);
        return Result.success();
    }

    @PostMapping("/{productId}/reviews/{reviewId}/like")
    public Result<Void> likeReview(@PathVariable Long productId, @PathVariable Long reviewId) {
        reviewCommandService.likeReview(reviewId);
        return Result.success();
    }
}
