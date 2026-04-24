package com.cartethyia.easyorange.product.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.product.application.command.*;
import com.cartethyia.easyorange.product.application.command.ProductCommandHandler;
import com.cartethyia.easyorange.product.dto.request.ProductCreateRequest;
import com.cartethyia.easyorange.product.dto.request.ProductUpdateRequest;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductCommandHandler commandHandler;

    @PostMapping
    public Result<ProductVO> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        CreateProductCommand command = CreateProductCommand.builder()
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .stock(request.getStock())
                .conditionLevel(request.getConditionLevel())
                .location(request.getLocation())
                .contactMethod(request.getContactMethod())
                .description(request.getDescription())
                .imageUrls(request.getImageUrls())
                .build();
        ProductVO product = commandHandler.handle(command);
        return Result.success(product);
    }

    @PutMapping("/{id}")
    public Result<ProductVO> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        UpdateProductCommand command = UpdateProductCommand.builder()
                .id(id)
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .stock(request.getStock())
                .conditionLevel(request.getConditionLevel())
                .location(request.getLocation())
                .contactMethod(request.getContactMethod())
                .description(request.getDescription())
                .imageUrls(request.getImageUrls())
                .build();
        ProductVO product = commandHandler.handle(command);
        return Result.success(product);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        DeleteProductCommand command = DeleteProductCommand.builder()
                .id(id)
                .build();
        commandHandler.handle(command);
        return Result.success();
    }

    @GetMapping("/my")
    public Result<PageResult<ProductVO>> getMyProducts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        // TODO: 后续通过QueryHandler实现
        return Result.success(PageResult.empty(pageNum, pageSize));
    }

    @PutMapping("/{productId}/decrement-stock")
    public Result<Void> decrementStock(@PathVariable Long productId) {
        commandHandler.handle(new DecrementStockCommand(productId));
        return Result.success();
    }

    @PutMapping("/{productId}/restore-stock")
    public Result<Void> restoreStock(@PathVariable Long productId) {
        commandHandler.handle(new RestoreStockCommand(productId));
        return Result.success();
    }

    @PutMapping("/{productId}/mark-sold")
    public Result<Void> markAsSold(@PathVariable Long productId) {
        commandHandler.handle(new MarkAsSoldCommand(productId));
        return Result.success();
    }
}