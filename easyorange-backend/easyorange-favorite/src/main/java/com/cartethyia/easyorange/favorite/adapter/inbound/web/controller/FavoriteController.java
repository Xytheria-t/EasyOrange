package com.cartethyia.easyorange.favorite.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
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
            @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Favorite> page = favoriteService.queryFavorites(pageNum, pageSize);
        return Result.success(favoriteAssembler.toPageResult(page, pageNum, pageSize));
    }

    @PostMapping("/{productId}")
    public Result<Void> addFavorite(@PathVariable String productId) {
        favoriteService.addFavorite(productId);
        return Result.success();
    }

    @DeleteMapping("/{productId}")
    public Result<Void> removeFavorite(@PathVariable String productId) {
        favoriteService.removeFavorite(productId);
        return Result.success();
    }

    @DeleteMapping("/batch")
    public Result<Void> removeManyFavorites(@Valid @RequestBody BatchRemoveRequest request) {
        favoriteService.removeManyFavorites(request.getIds());
        return Result.success();
    }

    @GetMapping("/check/{productId}")
    public Result<Boolean> checkIsFavorited(@PathVariable String productId) {
        return Result.success(favoriteService.isFavorited(productId));
    }

    @GetMapping("/count")
    public Result<Long> getFavoriteCount() {
        return Result.success(favoriteService.getFavoriteCount());
    }

    @PostMapping("/batch-check")
    public Result<Map<String, Boolean>> batchCheckFavorited(@Valid @RequestBody BatchCheckRequest request) {
        return Result.success(favoriteService.batchCheckFavorited(request.getProductIds()));
    }
}
