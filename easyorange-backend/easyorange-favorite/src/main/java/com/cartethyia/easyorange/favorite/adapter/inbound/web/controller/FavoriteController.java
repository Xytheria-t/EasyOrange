package com.cartethyia.easyorange.favorite.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.annotation.SkipRepeatSubmit;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.assembler.FavoriteAssembler;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.request.BatchCheckRequest;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.request.BatchRemoveRequest;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.response.FavoriteResponse;
import com.cartethyia.easyorange.favorite.application.service.FavoriteService;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "收藏", description = "商品收藏管理")
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final FavoriteAssembler favoriteAssembler;

    @GetMapping
    public Result<PageResult<FavoriteResponse>> getFavorites(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Favorite> page = favoriteService.queryFavorites(user.userId(), pageNum, pageSize);
        return Result.success(favoriteAssembler.toPageResult(page, pageNum, pageSize));
    }

    @PostMapping("/{productId}")
    public Result<Void> addFavorite(@AuthenticationPrincipal AuthUser user, @PathVariable String productId) {
        favoriteService.addFavorite(user.userId(), productId);
        return Result.success();
    }

    @DeleteMapping("/{productId}")
    public Result<Void> removeFavorite(@AuthenticationPrincipal AuthUser user, @PathVariable String productId) {
        favoriteService.removeFavorite(user.userId(), productId);
        return Result.success();
    }

    @DeleteMapping("/batch")
    public Result<Void> removeManyFavorites(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody BatchRemoveRequest request) {
        favoriteService.removeManyFavorites(user.userId(), request.ids());
        return Result.success();
    }

    @GetMapping("/check/{productId}")
    public Result<Boolean> checkIsFavorited(@AuthenticationPrincipal AuthUser user, @PathVariable String productId) {
        return Result.success(favoriteService.isFavorited(user.userId(), productId));
    }

    @GetMapping("/count")
    public Result<Long> getFavoriteCount(@AuthenticationPrincipal AuthUser user) {
        return Result.success(favoriteService.getFavoriteCount(user.userId()));
    }

    /**
     * 批量查询收藏状态 — 列表页/首页每次渲染都会调用，属只读语义（用 POST 仅为承载 id 列表）。
     * <p>
     * 必须跳过防重提交：防重 key 只含 IP + URI + body hash（不含方法），同一批 id 在 3s 窗口内
     * 重复查询（导航往返、筛选切换）会被判为重复提交返回 429，前端收藏状态随之整体丢失。
     */
    @SkipRepeatSubmit
    @PostMapping("/batch-check")
    public Result<Map<String, Boolean>> batchCheckFavorited(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody BatchCheckRequest request) {
        return Result.success(favoriteService.batchCheckFavorited(user.userId(), request.productIds()));
    }
}
