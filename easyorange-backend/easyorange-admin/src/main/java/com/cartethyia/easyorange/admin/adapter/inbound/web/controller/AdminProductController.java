package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminProductQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminProductResponse;
import com.cartethyia.easyorange.admin.service.AdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    public Result<PageResult<AdminProductResponse>> listProducts(AdminProductQueryRequest request) {
        return Result.success(adminProductService.listProducts(request));
    }

    @GetMapping("/{id}")
    public Result<AdminProductResponse> getProductDetail(@PathVariable String id) {
        return Result.success(adminProductService.getProductDetail(id));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateProductStatus(
        @PathVariable String id,
        @Valid @RequestBody UpdateStatusRequest request
    ) {
        adminProductService.updateProductStatus(id, request);
        return Result.success();
    }
}