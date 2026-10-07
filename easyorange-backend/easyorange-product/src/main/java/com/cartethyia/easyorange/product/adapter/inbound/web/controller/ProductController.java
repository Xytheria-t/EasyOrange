package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.annotation.SkipRepeatSubmit;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.product.adapter.inbound.web.assembler.CategoryAssembler;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductCreateRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductQueryRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductUpdateRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.product.application.command.CreateProductCommand;
import com.cartethyia.easyorange.product.application.command.ProductCommandHandler;
import com.cartethyia.easyorange.product.application.command.UpdateProductCommand;
import com.cartethyia.easyorange.product.application.port.cache.ViewCountPort;
import com.cartethyia.easyorange.product.application.query.CategoryQueryHandler;
import com.cartethyia.easyorange.product.application.query.ProductQueryHandler;
import com.cartethyia.easyorange.product.application.query.ProductSearchCriteria;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.domain.valueobject.AiSuggestion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "商品管理", description = "商品 CRUD/详情/分类")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductCommandHandler commandHandler;
    private final ViewCountPort viewCountPort;
    private final ProductQueryHandler queryHandler;
    private final CategoryQueryHandler categoryQueryHandler;
    private final CategoryAssembler categoryAssembler;

    // ── 写端点 ──

    @PostMapping
    @Operation(summary = "卖家发布资产草稿（DRAFT），同事务落库存基线流水")
    public Result<String> createProduct(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody ProductCreateRequest request) {
        var cmd = new CreateProductCommand(
                request.categoryId(),
                request.name(),
                request.price(),
                request.originalPrice(),
                request.stock(),
                request.conditionLevel(),
                request.location(),
                request.contactMethod(),
                request.description(),
                request.imageUrls(),
                toAiSuggestion(request.aiSuggestion()));
        return Result.success(commandHandler.createProduct(user.userId(), cmd));
    }

    /** 请求里的建议快照 → 领域值对象；整块缺失即 null（这单没走过拍照识别，不进采纳率分母）。 */
    private static AiSuggestion toAiSuggestion(ProductCreateRequest.AiSuggestionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new AiSuggestion(
                snapshot.title(),
                snapshot.description(),
                snapshot.price(),
                snapshot.categoryName(),
                snapshot.conditionLevel(),
                snapshot.location());
    }

    @PutMapping("/{id}")
    @Operation(summary = "资产方更新自己的资产，改动库存补记一条人工调整流水")
    public Result<Void> updateProduct(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable String id,
            @Valid @RequestBody ProductUpdateRequest request) {
        var cmd = new UpdateProductCommand(
                id,
                request.categoryId(),
                request.name(),
                request.price(),
                request.originalPrice(),
                request.stock(),
                request.conditionLevel(),
                request.location(),
                request.contactMethod(),
                request.description(),
                request.imageUrls());
        commandHandler.updateProduct(user.userId(), cmd);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "资产方删除自己的资产，SOLD 终端态拒删以保留订单商品记录")
    public Result<Void> deleteProduct(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        commandHandler.deleteProduct(user.userId(), id);
        return Result.success();
    }

    @PutMapping("/{id}/submit")
    @Operation(summary = "提交自己的资产审核（DRAFT / REJECTED → PENDING_REVIEW）")
    public Result<Void> submitForReview(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        commandHandler.submitForReview(user.userId(), id);
        return Result.success();
    }

    // 上架与下架对称：卖家重新上架自己的下架商品（C2C 直发，平台不代持货架）；
    // 管理员强制改状态走 /api/admin/products/{id}/status
    @PutMapping("/{productId}/online")
    @Operation(summary = "本人上架资产，进 ONLINE 前校验信息完整、价格与库存")
    public Result<Void> putOnline(@AuthenticationPrincipal AuthUser user, @PathVariable String productId) {
        commandHandler.putOnline(user.userId(), productId);
        return Result.success();
    }

    @PutMapping("/{productId}/offline")
    @Operation(summary = "本人下架在售资产（仅 ONLINE → OFFLINE 合法）")
    public Result<Void> takeOffline(@AuthenticationPrincipal AuthUser user, @PathVariable String productId) {
        commandHandler.takeOffline(user.userId(), productId);
        return Result.success();
    }

    @SkipRepeatSubmit
    @PostMapping("/{id}/view")
    @Operation(summary = "浏览量 +1（匿名可达，计数失败只告警不影响响应）")
    public Result<Void> incrementViewCount(@PathVariable String id) {
        if (id != null) {
            try {
                viewCountPort.increment(id);
            } catch (Exception e) {
                log.warn("记录浏览量失败: productId={}", id, e);
            }
        }
        return Result.success();
    }

    // ── 读端点 ──

    @GetMapping("/{id}")
    @Operation(summary = "公开资产详情，缓存优先并把空结果也缓存以防穿透")
    public Result<ProductVO> getProduct(@PathVariable String id) {
        return Result.success(queryHandler.getProductById(id));
    }

    @GetMapping
    @Operation(summary = "公开分页浏览资产，status 缺省时只返回 ONLINE 在售")
    public Result<PageResult<ProductVO>> listProducts(@Valid ProductQueryRequest request) {
        var criteria = new ProductSearchCriteria(
                request.getKeyword(),
                request.getCategoryId(),
                request.getStatus(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getConditionLevel(),
                request.getSort(),
                request.getHasDiscount(),
                request.getPageNum(),
                request.getPageSize());
        return Result.success(queryHandler.listProducts(criteria));
    }

    @GetMapping("/my")
    @Operation(summary = "当前登录卖家自己的资产列表，可按状态过滤分页")
    public Result<PageResult<ProductVO>> getMyProducts(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(queryHandler.getMyProducts(user.userId(), status, pageNum, pageSize));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "按类目浏览公开资产，强制覆盖请求里的 categoryId")
    public Result<PageResult<ProductVO>> getProductsByCategory(
            @PathVariable String categoryId, @Valid ProductQueryRequest request) {
        request.setCategoryId(categoryId);
        return listProducts(request);
    }

    @GetMapping("/{id}/similar")
    @Operation(summary = "同类目推荐（只取 ONLINE、排除自身，limit 默认 10）")
    public Result<List<ProductVO>> getSimilarProducts(
            @PathVariable String id, @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(queryHandler.getSimilarProducts(id, limit));
    }

    @PostMapping("/batch")
    @Operation(summary = "匿名批量取资产详情（按 ID，不做状态与归属过滤）")
    public Result<List<ProductVO>> getProductsByIds(@RequestBody List<String> ids) {
        return Result.success(queryHandler.getProductsByIds(ids));
    }

    @GetMapping("/categories")
    @Operation(summary = "分类列表（parentId 缺省取一级，商品计数含子分类聚合）")
    public Result<List<CategoryResponse>> getCategories(@RequestParam(required = false) String parentId) {
        var categories = categoryQueryHandler.getCategories(parentId);
        return Result.success(categoryAssembler.toCategoryResponses(categories));
    }
}
