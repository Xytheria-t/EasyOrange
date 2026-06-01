package com.cartethyia.easyorange.favorite.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.assembler.FavoriteAssembler;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.request.BatchCheckRequest;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.request.BatchRemoveRequest;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.response.FavoriteResponse;
import com.cartethyia.easyorange.favorite.application.service.FavoriteService;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final FavoriteAssembler favoriteAssembler;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<FavoriteResponse>> getFavorites(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Favorite> page = favoriteService.queryFavorites(pageNum, pageSize);
        return Result.success(favoriteAssembler.toPageResult(page, pageNum, pageSize));
    }

    @PostMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> addFavorite(@PathVariable Long productId) {
        favoriteService.addFavorite(productId);
        return Result.success();
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> removeFavorite(@PathVariable Long productId) {
        favoriteService.removeFavorite(productId);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> removeManyFavorites(@Valid @RequestBody BatchRemoveRequest request) {
        favoriteService.removeManyFavorites(request.getIds());
        return Result.success();
    }

    @GetMapping("/check/{productId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> checkIsFavorited(@PathVariable Long productId) {
        return Result.success(favoriteService.isFavorited(productId));
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public Result<Long> getFavoriteCount() {
        return Result.success(favoriteService.getFavoriteCount());
    }

    @PostMapping("/batch-check")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<Long, Boolean>> batchCheckFavorited(@Valid @RequestBody BatchCheckRequest request) {
        return Result.success(favoriteService.batchCheckFavorited(request.getProductIds()));
    }
}