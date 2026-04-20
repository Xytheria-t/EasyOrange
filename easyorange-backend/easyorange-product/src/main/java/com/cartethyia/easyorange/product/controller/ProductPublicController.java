package com.cartethyia.easyorange.product.controller;

import com.cartethyia.easyorange.common.annotation.RateLimiter;
import com.cartethyia.easyorange.common.enums.LimitType;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.product.application.handler.ProductQueryHandler;
import com.cartethyia.easyorange.product.application.query.ProductQuery;
import com.cartethyia.easyorange.product.dto.request.ProductQueryRequest;
import com.cartethyia.easyorange.product.dto.vo.CategoryVO;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductPublicController {

    private final ProductQueryHandler queryHandler;

    @GetMapping
    @RateLimiter(key = "product_list", count = 30, time = 60, limitType = LimitType.IP)
    public Result<PageResult<ProductVO>> listProducts(@Valid ProductQueryRequest request) {
        PageResult<ProductVO> page = queryHandler.handle(request);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @RateLimiter(key = "product_detail", count = 60, time = 60, limitType = LimitType.IP)
    public Result<ProductVO> getProduct(@PathVariable Long id) {
        ProductVO product = queryHandler.handle(ProductQuery.builder().id(id).build());
        return Result.success(product);
    }

    @GetMapping("/category/{categoryId}")
    public Result<PageResult<ProductVO>> getProductsByCategory(
            @PathVariable Long categoryId,
            @Valid ProductQueryRequest request) {
        request.setCategoryId(categoryId);
        PageResult<ProductVO> page = queryHandler.handle(request);
        return Result.success(page);
    }

    @GetMapping("/{id}/similar")
    public Result<List<ProductVO>> getSimilarProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<ProductVO> products = queryHandler.handleSimilarProducts(id, limit);
        return Result.success(products);
    }

    @PostMapping("/batch")
    public Result<List<ProductVO>> getProductsByIds(@RequestBody List<Long> ids) {
        // TODO: 后续通过QueryHandler实现
        return Result.success(List.of());
    }

    @PostMapping("/{id}/view")
    public Result<Void> incrementView(@PathVariable Long id) {
        // TODO: 后续通过QueryHandler实现
        return Result.success();
    }

    @GetMapping("/categories")
    @RateLimiter(key = "category_list", count = 30, time = 60, limitType = LimitType.IP)
    public Result<List<CategoryVO>> getCategories(
            @RequestParam(required = false) Long parentId) {
        // TODO: 后续通过QueryHandler实现
        return Result.success(List.of());
    }
}