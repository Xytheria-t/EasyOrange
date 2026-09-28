package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryCreateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryUpdateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.admin.service.AdminCategoryService;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台-分类", description = "商品分类管理")
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    /**
     * 分类列表（含禁用）。
     *
     * @param parentId 父分类 id；不传返回一级分类
     */
    @GetMapping
    public Result<List<CategoryResponse>> listCategories(@RequestParam(required = false) String parentId) {
        return Result.success(adminCategoryService.listCategories(parentId));
    }

    /**
     * 整棵分类树（仅启用中）—— 路径保留 {@code /tree} 以兼容既有前端调用方。
     * <p>
     * 节点结构与列表接口共用 {@code CategoryResponse}（children 字段非空），
     * 不再单独维护一套 {@code CategoryTreeResponse}。
     */
    @GetMapping("/tree")
    public Result<List<CategoryResponse>> categoryTree() {
        return Result.success(adminCategoryService.categoryTree());
    }

    @PostMapping
    public Result<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return Result.success(adminCategoryService.createCategory(request));
    }

    /** 更新分类；请求体带 parentId 且与当前不同即视为移动挂载点。 */
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
