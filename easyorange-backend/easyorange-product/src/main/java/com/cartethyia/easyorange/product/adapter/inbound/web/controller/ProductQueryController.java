package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.application.query.CategoryQueryService;
import com.cartethyia.easyorange.product.application.query.ProductQueryService;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductQueryRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.assembler.CategoryAssembler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductQueryController {

    private final ProductQueryService queryService;
    private final CategoryQueryService categoryQueryService;
    private final CategoryAssembler categoryAssembler;

    @GetMapping
    public Result<PageResult<ProductVO>> listProducts(@Valid ProductQueryRequest request) {
        return Result.success(queryService.listProducts(
                request.getKeyword(), request.getCategoryId(), request.getStatus(),
                request.getMinPrice(), request.getMaxPrice(),
                request.getConditionLevel(), request.getSort(),
                request.getHasDiscount(),
                request.getPageNum(), request.getPageSize()));
    }

    @GetMapping("/{id}")
    public Result<ProductVO> getProduct(@PathVariable Long id) {
        return Result.success(queryService.getProductById(id));
    }

    @GetMapping("/my")
    public Result<PageResult<ProductVO>> getMyProducts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return Result.success(queryService.getMyProducts(currentUserId, status, pageNum, pageSize));
    }

    @GetMapping("/category/{categoryId}")
    public Result<PageResult<ProductVO>> getProductsByCategory(
            @PathVariable Long categoryId,
            @Valid ProductQueryRequest request) {
        return Result.success(queryService.listProducts(
                request.getKeyword(), categoryId, request.getStatus(),
                request.getMinPrice(), request.getMaxPrice(),
                request.getConditionLevel(), request.getSort(),
                request.getHasDiscount(),
                request.getPageNum(), request.getPageSize()));
    }

    @GetMapping("/{id}/similar")
    public Result<List<ProductVO>> getSimilarProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(queryService.getSimilarProducts(id, limit));
    }

    @PostMapping("/batch")
    public Result<List<ProductVO>> getProductsByIds(@RequestBody List<Long> ids) {
        return Result.success(queryService.getProductsByIds(ids));
    }

    @GetMapping("/categories")
    public Result<List<CategoryResponse>> getCategories(
            @RequestParam(required = false) Long parentId) {
        var categories = categoryQueryService.getCategories(parentId);
        return Result.success(categoryAssembler.toCategoryResponses(categories));
    }

}
