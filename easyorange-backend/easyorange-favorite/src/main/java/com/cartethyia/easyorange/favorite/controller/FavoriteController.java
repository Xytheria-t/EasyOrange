package com.cartethyia.easyorange.favorite.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.favorite.controller.request.BatchCheckRequest;
import com.cartethyia.easyorange.favorite.controller.request.BatchRemoveRequest;
import com.cartethyia.easyorange.favorite.service.FavoriteService;
import com.cartethyia.easyorange.favorite.service.dto.AddFavoriteDTO;
import com.cartethyia.easyorange.favorite.service.dto.FavoritePageQuery;
import com.cartethyia.easyorange.favorite.service.dto.FavoriteVO;
import com.cartethyia.easyorange.favorite.service.dto.RemoveFavoriteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Validated
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<FavoriteVO>> getFavorites(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        FavoritePageQuery query = FavoritePageQuery.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        return Result.success(favoriteService.queryFavorites(query));
    }

    @PostMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> addFavorite(@PathVariable Long productId) {
        favoriteService.addFavorite(AddFavoriteDTO.builder().productId(productId).build());
        return Result.success();
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> removeFavorite(@PathVariable Long productId) {
        favoriteService.removeFavorite(RemoveFavoriteDTO.builder().productId(productId).build());
        return Result.success();
    }

    @DeleteMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> removeManyFavorites(@RequestBody @Validated BatchRemoveRequest request) {
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
    public Result<Map<Long, Boolean>> batchCheckFavorited(@RequestBody @Validated BatchCheckRequest request) {
        return Result.success(favoriteService.batchCheckFavorited(request.getProductIds()));
    }
}
