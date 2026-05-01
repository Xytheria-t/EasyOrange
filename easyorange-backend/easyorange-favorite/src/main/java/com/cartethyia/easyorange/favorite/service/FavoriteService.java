package com.cartethyia.easyorange.favorite.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import com.cartethyia.easyorange.favorite.domain.repository.FavoriteRepository;
import com.cartethyia.easyorange.favorite.infrastructure.acl.ProductAclService;
import com.cartethyia.easyorange.favorite.service.dto.AddFavoriteDTO;
import com.cartethyia.easyorange.favorite.service.dto.FavoritePageQuery;
import com.cartethyia.easyorange.favorite.service.dto.RemoveFavoriteDTO;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private static final Logger log = LoggerFactory.getLogger(FavoriteService.class);

    private final FavoriteRepository favoriteRepository;
    private final ProductAclService productAclService;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           ProductAclService productAclService) {
        this.favoriteRepository = favoriteRepository;
        this.productAclService = productAclService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(AddFavoriteDTO dto) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        Long productId = dto.getProductId();

        BizRequire.requireTrue(productAclService.productExists(productId), "商品不存在");
        BizRequire.requireFalse(productAclService.isOwnProduct(userId, productId), "不能收藏自己的商品");
        BizRequire.requireFalse(
                favoriteRepository.existsByUserIdAndProductId(userId, productId),
                "已收藏过该商品"
        );

        Favorite favorite = Favorite.create(userId, productId);
        favoriteRepository.save(favorite);

        log.info("添加收藏成功: userId={}, productId={}", userId, productId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(RemoveFavoriteDTO dto) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        Long productId = dto.getProductId();

        Favorite favorite = favoriteRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> BusinessException.of("未收藏过该商品"));

        favorite.validateOwnership(userId);
        favoriteRepository.removeById(favorite.getId());
        log.info("移除收藏成功: userId={}, productId={}", userId, productId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeManyFavorites(List<Long> ids) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Favorite> favorites = favoriteRepository.findByIds(ids);
        favorites.forEach(favorite -> favorite.validateOwnership(userId));

        int removedCount = favoriteRepository.removeByIds(ids);
        log.info("批量移除收藏成功: userId={}, count={}", userId, removedCount);
    }

    public PageResult<ProductVO> queryFavorites(FavoritePageQuery query) {
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

        List<ProductReadModel> products = productAclService.findProductsByIds(productIds);
        if (products.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        Set<Long> sellerIds = products.stream()
                .map(ProductReadModel::sellerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, SellerReadModel> sellerMap = productAclService.findSellersByIds(sellerIds);

        List<ProductVO> voList = productAclService.assembleProductVOs(products, sellerMap);

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
}
