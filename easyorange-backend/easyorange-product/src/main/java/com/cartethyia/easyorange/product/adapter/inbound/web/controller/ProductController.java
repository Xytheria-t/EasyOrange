package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.annotation.SkipRepeatSubmit;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.adapter.inbound.web.assembler.CategoryAssembler;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductCreateRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductQueryRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductUpdateRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.product.application.command.CreateProductCommand;
import com.cartethyia.easyorange.product.application.command.ProductCommandHandler;
import com.cartethyia.easyorange.product.application.command.UpdateProductCommand;
import com.cartethyia.easyorange.product.application.query.CategoryQueryHandler;
import com.cartethyia.easyorange.product.application.query.ProductQueryHandler;
import com.cartethyia.easyorange.product.application.query.ProductSearchCriteria;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.application.service.ProductViewCountAppService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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

@Slf4j
@Tag(name = "商品管理", description = "商品 CRUD/详情/分类")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductCommandHandler commandHandler;
    private final ProductViewCountAppService viewCountService;
    private final ProductQueryHandler queryHandler;
    private final CategoryQueryHandler categoryQueryHandler;
    private final CategoryAssembler categoryAssembler;

    // ==================== Commands ====================

    /**
     * Resource CRUD
     */
    @PostMapping
    public Result<String> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        var cmd = new CreateProductCommand(
                request.categoryId(),
                request.name(),
                request.price(),
                request.originalPrice(),
                request.stock(),
                request.conditionLevel(),
                request.location(),
                request.contactMethod(),
                request.description(),
                request.imageUrls());
        return Result.success(commandHandler.createProduct(cmd));
    }

    @PutMapping("/{id}")
    public Result<Void> updateProduct(@PathVariable String id, @Valid @RequestBody ProductUpdateRequest request) {
        var cmd = new UpdateProductCommand(
                id,
                request.categoryId(),
                request.name(),
                request.price(),
                request.originalPrice(),
                request.stock(),
                request.conditionLevel(),
                request.location(),
                request.contactMethod(),
                request.description(),
                request.imageUrls());
        commandHandler.updateProduct(cmd);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable String id) {
        commandHandler.deleteProduct(id);
        return Result.success();
    }

    /**
     * Product state transitions (lifecycle order)
     */
    @PutMapping("/{id}/submit")
    public Result<Void> submitForReview(@PathVariable String id) {
        commandHandler.submitForReview(id);
        return Result.success();
    }

    @PutMapping("/{productId}/online")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> putOnline(@PathVariable String productId) {
        commandHandler.putOnline(productId);
        return Result.success();
    }

    @PutMapping("/{productId}/offline")
    public Result<Void> takeOffline(@PathVariable String productId) {
        commandHandler.takeOffline(productId);
        return Result.success();
    }

    @PutMapping("/{productId}/mark-sold")
    public Result<Void> markAsSold(@PathVariable String productId) {
        commandHandler.markAsSold(productId);
        return Result.success();
    }

    /**
     * Stock operations
     */
    @PutMapping("/{productId}/decrement-stock")
    public Result<Void> decrementStock(@PathVariable String productId) {
        commandHandler.decrementStock(productId, 1);
        return Result.success();
    }

    @PutMapping("/{productId}/restore-stock")
    public Result<Void> restoreStock(@PathVariable String productId) {
        commandHandler.restoreStock(productId);
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
        return Result.success(queryHandler.getProductById(id));
    }

    /**
     * Product listing and filtered queries
     */
    @GetMapping
    public Result<PageResult<ProductVO>> listProducts(@Valid ProductQueryRequest request) {
        var criteria = new ProductSearchCriteria(
                request.getKeyword(),
                request.getCategoryId(),
                request.getStatus(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getConditionLevel(),
                request.getSort(),
                request.getHasDiscount(),
                request.getPageNum(),
                request.getPageSize());
        return Result.success(queryHandler.listProducts(criteria));
    }

    @GetMapping("/my")
    public Result<PageResult<ProductVO>> getMyProducts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String status) {
        String currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return Result.success(queryHandler.getMyProducts(currentUserId, status, pageNum, pageSize));
    }

    @GetMapping("/category/{categoryId}")
    public Result<PageResult<ProductVO>> getProductsByCategory(
            @PathVariable String categoryId, @Valid ProductQueryRequest request) {
        request.setCategoryId(categoryId);
        return listProducts(request);
    }

    @GetMapping("/{id}/similar")
    public Result<List<ProductVO>> getSimilarProducts(
            @PathVariable String id, @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(queryHandler.getSimilarProducts(id, limit));
    }

    @PostMapping("/batch")
    public Result<List<ProductVO>> getProductsByIds(@RequestBody List<String> ids) {
        return Result.success(queryHandler.getProductsByIds(ids));
    }

    @GetMapping("/categories")
    public Result<List<CategoryResponse>> getCategories(@RequestParam(required = false) String parentId) {
        var categories = categoryQueryHandler.getCategories(parentId);
        return Result.success(categoryAssembler.toCategoryResponses(categories));
    }
}
