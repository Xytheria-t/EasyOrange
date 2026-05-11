package com.cartethyia.easyorange.admin.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.admin.dto.request.CategoryCreateRequest;
import com.cartethyia.easyorange.admin.dto.request.CategoryUpdateRequest;
import com.cartethyia.easyorange.admin.dto.response.CategoryTreeVO;
import com.cartethyia.easyorange.admin.dto.response.CategoryVO;
import com.cartethyia.easyorange.admin.service.AdminCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    public Result<List<CategoryVO>> listCategories(@RequestParam(required = false) Long parentId) {
        return Result.success(adminCategoryService.listCategories(parentId));
    }

    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> categoryTree() {
        return Result.success(adminCategoryService.categoryTree());
    }

    @PostMapping
    public Result<CategoryVO> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return Result.success(adminCategoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    public Result<CategoryVO> updateCategory(
        @PathVariable Long id,
        @Valid @RequestBody CategoryUpdateRequest request
    ) {
        return Result.success(adminCategoryService.updateCategory(id, request));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminCategoryService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        adminCategoryService.deleteCategory(id);
        return Result.success();
    }
}
