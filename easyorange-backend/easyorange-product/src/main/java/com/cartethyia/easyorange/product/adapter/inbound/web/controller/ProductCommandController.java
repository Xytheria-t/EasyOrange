package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.product.application.command.ProductCommandService;
import com.cartethyia.easyorange.product.application.command.dto.CreateProductCommand;
import com.cartethyia.easyorange.product.application.command.dto.DecrementStockCommand;
import com.cartethyia.easyorange.product.application.command.dto.DeleteProductCommand;
import com.cartethyia.easyorange.product.application.command.dto.MarkAsSoldCommand;
import com.cartethyia.easyorange.product.application.service.ProductViewCountService;
import com.cartethyia.easyorange.product.application.command.dto.RestoreStockCommand;
import com.cartethyia.easyorange.product.application.command.dto.UpdateProductCommand;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductCreateRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductCommandController {

    private final ProductCommandService commandService;
    private final ProductViewCountService viewCountService;

    @PostMapping
    public Result<Long> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return Result.success(commandService.createProduct(CreateProductCommand.from(request)));
    }

    @PutMapping("/{id}")
    public Result<Void> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        commandService.updateProduct(UpdateProductCommand.from(id, request));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        commandService.deleteProduct(DeleteProductCommand.from(id));
        return Result.success();
    }

    @PostMapping("/{id}/view")
    public Result<Void> incrementView(@PathVariable Long id) {
        viewCountService.incrementViewCount(id);
        return Result.success();
    }

    @PutMapping("/{productId}/decrement-stock")
    public Result<Void> decrementStock(@PathVariable Long productId) {
        commandService.decrementStock(new DecrementStockCommand(productId, 1));
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

    @PutMapping("/{id}/submit")
    public Result<Void> submitForReview(@PathVariable Long id) {
        commandService.submitForReview(id);
        return Result.success();
    }
}
