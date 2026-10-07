package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminCategoryAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryCreateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryUpdateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.admin.application.service.AdminCategoryAppService;
import com.cartethyia.easyorange.admin.domain.model.CategoryUpdateCommand;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
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

    private final AdminCategoryAppService adminCategoryService;
    private final AdminCategoryAssembler assembler;

    /**
     * 分类列表（含禁用）。
     *
     * @param parentId 父分类 id；不传返回一级分类
     */
    @GetMapping
    @Operation(summary = "某父分类下的直属子分类，含禁用项；不传 parentId 查一级")
    public Result<List<CategoryResponse>> listCategories(@RequestParam(required = false) String parentId) {
        return Result.success(assembler.toResponses(adminCategoryService.listCategories(parentId)));
    }

    /**
     * 整棵分类树（仅启用中）—— 路径保留 {@code /tree} 以兼容既有前端调用方。
     * <p>
     * 节点结构与列表接口共用 {@code CategoryResponse}（children 字段非空），
     * 不再单独维护一套 {@code CategoryTreeResponse}。
     */
    @GetMapping("/tree")
    @Operation(summary = "仅启用中的完整分类树；停用分类不出现在此接口")
    public Result<List<CategoryResponse>> categoryTree() {
        return Result.success(assembler.toResponses(adminCategoryService.categoryTree()));
    }

    @PostMapping
    @Operation(summary = "新建分类；不传 parentId 建一级，层级与重名由 product 侧裁决")
    public Result<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return Result.success(assembler.toResponse(adminCategoryService.createCategory(
                request.name(), request.parentId(), request.icon(), request.sortOrder())));
    }

    /** 更新分类；请求体带 parentId 且与当前不同即视为移动挂载点。 */
    @PutMapping("/{id}")
    @Operation(summary = "更新分类属性；parentId 变更时另走移动路径，连带平移子树层级")
    public Result<CategoryResponse> updateCategory(
            @PathVariable String id, @Valid @RequestBody CategoryUpdateRequest request) {
        return Result.success(assembler.toResponse(adminCategoryService.updateCategory(id, toCommand(request))));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用 / 禁用分类（status：1 启用，0 禁用）")
    public Result<Void> updateStatus(@PathVariable String id, @RequestParam Integer status) {
        adminCategoryService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类；有子分类或关联商品时由领域层拒绝")
    public Result<Void> deleteCategory(@PathVariable String id) {
        adminCategoryService.deleteCategory(id);
        return Result.success();
    }

    private static CategoryUpdateCommand toCommand(CategoryUpdateRequest request) {
        return new CategoryUpdateCommand(
                request.name(), request.parentId(), request.icon(), request.sortOrder(), request.status());
    }
}
