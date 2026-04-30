package com.cartethyia.easyorange.product.interfaces.rest;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.product.application.command.ProductCommandService;
import com.cartethyia.easyorange.product.application.command.dto.CreateProductCommand;
import com.cartethyia.easyorange.product.application.command.dto.DecrementStockCommand;
import com.cartethyia.easyorange.product.application.command.dto.DeleteProductCommand;
import com.cartethyia.easyorange.product.application.command.dto.MarkAsSoldCommand;
import com.cartethyia.easyorange.product.application.command.dto.RestoreStockCommand;
import com.cartethyia.easyorange.product.application.command.dto.UpdateProductCommand;
import com.cartethyia.easyorange.product.interfaces.rest.dto.request.ProductCreateRequest;
import com.cartethyia.easyorange.product.interfaces.rest.dto.request.ProductUpdateRequest;
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

    private final ProductCommandService commandService;

    @PostMapping
    public Result<Long> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        Long productId = commandService.createProduct(CreateProductCommand.from(request));
        return Result.success(productId);
    }

    @PutMapping("/{id}")
    public Result<Long> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        Long productId = commandService.updateProduct(UpdateProductCommand.from(id, request));
        return Result.success(productId);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        DeleteProductCommand command = DeleteProductCommand.builder()
                .id(id)
                .build();
        commandService.deleteProduct(command);
        return Result.success();
    }

    @PutMapping("/{productId}/decrement-stock")
    public Result<Void> decrementStock(@PathVariable Long productId) {
        commandService.decrementStock(new DecrementStockCommand(productId));
        return Result.success();
    }

    @PutMapping("/{productId}/restore-stock")
    public Result<Void> restoreStock(@PathVariable Long productId) {
        commandService.restoreStock(new RestoreStockCommand(productId));
        return Result.success();
    }

    @PutMapping("/{productId}/mark-sold")
    public Result<Void> markAsSold(@PathVariable Long productId) {
        commandService.markAsSold(new MarkAsSoldCommand(productId));
        return Result.success();
    }

    @PutMapping("/{productId}/online")
    public Result<Void> putOnline(@PathVariable Long productId) {
        commandService.putOnline(productId);
        return Result.success();
    }

    @PutMapping("/{productId}/offline")
    public Result<Void> takeOffline(@PathVariable Long productId) {
        commandService.takeOffline(productId);
        return Result.success();
    }
}
