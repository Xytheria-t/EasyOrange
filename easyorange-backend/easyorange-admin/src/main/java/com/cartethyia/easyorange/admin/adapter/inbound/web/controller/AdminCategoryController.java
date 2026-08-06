package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryCreateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryUpdateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryTreeResponse;
import com.cartethyia.easyorange.admin.service.AdminCategoryService;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-分类", description = "商品分类管理")
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    public Result<List<CategoryResponse>> listCategories(@RequestParam(required = false) String parentId) {
        return Result.success(adminCategoryService.listCategories(parentId));
    }

    @GetMapping("/tree")
    public Result<List<CategoryTreeResponse>> categoryTree() {
        return Result.success(adminCategoryService.categoryTree());
    }

    @PostMapping
    public Result<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return Result.success(adminCategoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    public Result<CategoryResponse> updateCategory(
            @PathVariable String id, @Valid @RequestBody CategoryUpdateRequest request) {
        return Result.success(adminCategoryService.updateCategory(id, request));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestParam Integer status) {
        adminCategoryService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable String id) {
        adminCategoryService.deleteCategory(id);
        return Result.success();
    }
}
