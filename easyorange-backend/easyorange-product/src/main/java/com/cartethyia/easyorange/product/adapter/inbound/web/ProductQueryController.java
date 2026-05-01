package com.cartethyia.easyorange.product.adapter.inbound.web;

import com.cartethyia.easyorange.common.annotation.RateLimiter;
import com.cartethyia.easyorange.common.enums.LimitType;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.application.query.ProductQueryService;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductQueryRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.CategoryResponse;
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

    @GetMapping
    @RateLimiter(key = "product_list", count = 30, time = 60, limitType = LimitType.IP)
    public Result<PageResult<ProductVO>> listProducts(@Valid ProductQueryRequest request) {
        return Result.success(queryService.listProducts(request));
    }

    @GetMapping("/{id}")
    @RateLimiter(key = "product_detail", count = 60, time = 60, limitType = LimitType.IP)
    public Result<ProductVO> getProduct(@PathVariable Long id) {
        ProductVO product = queryService.getProductById(id);
        return Result.success(product);
    }

    @GetMapping("/my")
    public Result<PageResult<ProductVO>> getMyProducts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return Result.success(queryService.getMyProducts(currentUserId, pageNum, pageSize));
    }

    @GetMapping("/category/{categoryId}")
    public Result<PageResult<ProductVO>> getProductsByCategory(
            @PathVariable Long categoryId,
            @Valid ProductQueryRequest request) {
        request.setCategoryId(categoryId);
        return Result.success(queryService.listProducts(request));
    }

    @GetMapping("/{id}/similar")
    public Result<List<ProductVO>> getSimilarProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<ProductVO> products = queryService.getSimilarProducts(id, limit);
        return Result.success(products);
    }

    @PostMapping("/batch")
    public Result<List<ProductVO>> getProductsByIds(@RequestBody List<Long> ids) {
        List<ProductVO> products = queryService.getProductsByIds(ids);
        return Result.success(products);
    }

    @PostMapping("/{id}/view")
    public Result<Void> incrementView(@PathVariable Long id) {
        queryService.incrementViewCount(id);
        return Result.success();
    }

    @GetMapping("/categories")
    @RateLimiter(key = "category_list", count = 30, time = 60, limitType = LimitType.IP)
    public Result<List<CategoryResponse>> getCategories(
            @RequestParam(required = false) Long parentId) {
        return Result.success(queryService.getCategories(parentId));
    }
}
