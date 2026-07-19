package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.annotation.Idempotent;
import com.cartethyia.easyorange.common.annotation.SkipRepeatSubmit;
import org.springframework.security.access.prepost.PreAuthorize;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.adapter.inbound.web.assembler.CategoryAssembler;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductCreateRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductQueryRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductUpdateRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.product.application.command.ProductCommandService;
import com.cartethyia.easyorange.product.application.query.CategoryQueryService;
import com.cartethyia.easyorange.product.application.query.ProductQueryService;
import com.cartethyia.easyorange.product.application.query.ProductVO;
import com.cartethyia.easyorange.product.application.service.ProductViewCountAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductCommandService commandService;
    private final ProductViewCountAppService viewCountService;
    private final ProductQueryService queryService;
    private final CategoryQueryService categoryQueryService;
    private final CategoryAssembler categoryAssembler;

    // ==================== Commands ====================

    /**
     * Resource CRUD
     */
    @PostMapping
    @Idempotent
    public Result<String> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        var cmd = new ProductCommandService.CreateProductCommand(
                request.getCategoryId(), request.getName(), request.getPrice(),
                request.getOriginalPrice(), request.getStock(), request.getConditionLevel(),
                request.getLocation(), request.getContactMethod(), request.getDescription(),
                request.getImageUrls());
        return Result.success(commandService.createProduct(cmd));
    }

    @PutMapping("/{id}")
    public Result<Void> updateProduct(@PathVariable String id, @Valid @RequestBody ProductUpdateRequest request) {
        var cmd = new ProductCommandService.UpdateProductCommand(
                id, request.getCategoryId(), request.getName(), request.getPrice(),
                request.getOriginalPrice(), request.getStock(), request.getConditionLevel(),
                request.getLocation(), request.getContactMethod(), request.getDescription(),
                request.getImageUrls());
        commandService.updateProduct(cmd);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable String id) {
        commandService.deleteProduct(id);
        return Result.success();
    }

    /**
     * Product state transitions (lifecycle order)
     */
    @PutMapping("/{id}/submit")
    public Result<Void> submitForReview(@PathVariable String id) {
        commandService.submitForReview(id);
        return Result.success();
    }

    @PutMapping("/{productId}/online")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> putOnline(@PathVariable String productId) {
        commandService.putOnline(productId);
        return Result.success();
    }

    @PutMapping("/{productId}/offline")
    public Result<Void> takeOffline(@PathVariable String productId) {
        commandService.takeOffline(productId);
        return Result.success();
    }

    @PutMapping("/{productId}/mark-sold")
    public Result<Void> markAsSold(@PathVariable String productId) {
        commandService.markAsSold(productId);
        return Result.success();
    }

    /**
     * Stock operations
     */
    @PutMapping("/{productId}/decrement-stock")
    public Result<Void> decrementStock(@PathVariable String productId) {
        commandService.decrementStock(productId, 1);
        return Result.success();
    }

    @PutMapping("/{productId}/restore-stock")
    public Result<Void> restoreStock(@PathVariable String productId) {
        commandService.restoreStock(productId);
        return Result.success();
    }

    /**
     * View count tracking
     */
    @SkipRepeatSubmit
    @PostMapping("/{id}/view")
    public Result<Void> incrementViewCount(@PathVariable String id) {
        viewCountService.incrementViewCount(id);
        return Result.success();
    }

    // ==================== Queries ====================

    /**
     * Single resource lookup
     */
    @GetMapping("/{id}")
    public Result<ProductVO> getProduct(@PathVariable String id) {
        return Result.success(queryService.getProductById(id));
    }

    /**
     * Product listing and filtered queries
     */
    @GetMapping
    public Result<PageResult<ProductVO>> listProducts(@Valid ProductQueryRequest request) {
        return Result.success(queryService.listProducts(
                request.getKeyword(), request.getCategoryId(), request.getStatus(),
                request.getMinPrice(), request.getMaxPrice(),
                request.getConditionLevel(), request.getSort(),
                request.getHasDiscount(),
                request.getPageNum(), request.getPageSize()));
    }

    @GetMapping("/my")
    public Result<PageResult<ProductVO>> getMyProducts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        String currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return Result.success(queryService.getMyProducts(currentUserId, status, pageNum, pageSize));
    }

    @GetMapping("/category/{categoryId}")
    public Result<PageResult<ProductVO>> getProductsByCategory(
            @PathVariable String categoryId,
            @Valid ProductQueryRequest request) {
        request.setCategoryId(categoryId);
        return listProducts(request);
    }

    @GetMapping("/{id}/similar")
    public Result<List<ProductVO>> getSimilarProducts(
            @PathVariable String id,
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(queryService.getSimilarProducts(id, limit));
    }

    @PostMapping("/batch")
    public Result<List<ProductVO>> getProductsByIds(@RequestBody List<String> ids) {
        return Result.success(queryService.getProductsByIds(ids));
    }

    @GetMapping("/categories")
    public Result<List<CategoryResponse>> getCategories(
            @RequestParam(required = false) String parentId) {
        var categories = categoryQueryService.getCategories(parentId);
        return Result.success(categoryAssembler.toCategoryResponses(categories));
    }
}
