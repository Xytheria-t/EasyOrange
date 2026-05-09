package com.cartethyia.easyorange.favorite.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import com.cartethyia.easyorange.favorite.domain.port.output.ProductInfoPort;
import com.cartethyia.easyorange.favorite.domain.repository.FavoriteRepository;
import com.cartethyia.easyorange.favorite.domain.valueobject.ProductDetailInfo;
import com.cartethyia.easyorange.favorite.domain.valueobject.ProductInfo;
import com.cartethyia.easyorange.favorite.domain.valueobject.SellerInfo;
import com.cartethyia.easyorange.favorite.service.dto.AddFavoriteDTO;
import com.cartethyia.easyorange.favorite.service.dto.FavoritePageQuery;
import com.cartethyia.easyorange.favorite.service.dto.FavoriteVO;
import com.cartethyia.easyorange.favorite.service.dto.RemoveFavoriteDTO;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductInfoPort productInfoPort;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           ProductInfoPort productInfoPort) {
        this.favoriteRepository = favoriteRepository;
        this.productInfoPort = productInfoPort;
    }

    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(AddFavoriteDTO dto) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        Long productId = dto.getProductId();

        BizRequire.requireTrue(productInfoPort.productExists(productId), "商品不存在");
        BizRequire.requireFalse(productInfoPort.isOwnProduct(userId, productId), "不能收藏自己的商品");
        BizRequire.requireFalse(
                favoriteRepository.existsByUserIdAndProductId(userId, productId),
                "已收藏过该商品"
        );

        Favorite favorite = Favorite.create(userId, productId);
        favoriteRepository.save(favorite);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(RemoveFavoriteDTO dto) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        Long productId = dto.getProductId();

        Favorite favorite = favoriteRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> BusinessException.of("未收藏过该商品"));

        favorite.validateOwnership(userId);
        favoriteRepository.removeById(favorite.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeManyFavorites(List<Long> ids) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Favorite> favorites = favoriteRepository.findByIds(ids);
        favorites.forEach(favorite -> favorite.validateOwnership(userId));

        favoriteRepository.removeByIds(ids);
    }

    public PageResult<FavoriteVO> queryFavorites(FavoritePageQuery query) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 10;
        long offset = (pageNum - 1L) * pageSize;

        long total = favoriteRepository.countByUserId(userId);
        List<Favorite> favorites = favoriteRepository.findByUserId(userId, offset, pageSize);

        if (favorites.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        List<Long> productIds = favorites.stream()
                .map(Favorite::getProductId)
                .collect(Collectors.toList());

        List<ProductInfo> products = productInfoPort.findProductsByIds(productIds);
        if (products.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        Set<Long> sellerIds = products.stream()
                .map(ProductInfo::sellerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, SellerInfo> sellerMap = productInfoPort.findSellersByIds(sellerIds);

        List<ProductDetailInfo> productDetailInfos = productInfoPort.assembleProductDetails(products, sellerMap);

        Map<Long, ProductDetailInfo> productDetailMap = productDetailInfos.stream()
                .collect(Collectors.toMap(ProductDetailInfo::id, p -> p, (a, b) -> a));

        Map<Long, Favorite> favoriteByProductId = favorites.stream()
                .collect(Collectors.toMap(Favorite::getProductId, f -> f, (a, b) -> a));

        List<FavoriteVO> voList = productIds.stream()
                .map(productId -> {
                    Favorite fav = favoriteByProductId.get(productId);
                    ProductDetailInfo productDetail = productDetailMap.get(productId);
                    if (fav == null || productDetail == null) {
                        return null;
                    }
                    return FavoriteVO.builder()
                            .id(fav.getId())
                            .productId(fav.getProductId())
                            .product(productDetail)
                            .createTime(fav.getCreateTime())
                            .build();
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());

        return PageResult.of(voList, total, pageNum, pageSize);
    }

    public boolean isFavorited(Long productId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return favoriteRepository.existsByUserIdAndProductId(userId, productId);
    }

    public long getFavoriteCount() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return favoriteRepository.countByUserId(userId);
    }

    public Map<Long, Boolean> batchCheckFavorited(List<Long> productIds) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> favoritedIds = favoriteRepository.findFavoritedProductIds(userId, productIds);
        return productIds.stream()
                .collect(Collectors.toMap(pid -> pid, favoritedIds::contains, (a, b) -> a, LinkedHashMap::new));
    }
}
