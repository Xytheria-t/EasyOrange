package com.cartethyia.easyorange.favorite.adapter.outbound.persistence;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import com.cartethyia.easyorange.favorite.domain.repository.FavoriteRepository;
import com.cartethyia.easyorange.framework.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class MybatisFavoriteRepository extends BaseRepository<FavoriteMapper, FavoriteDO> implements FavoriteRepository {

    public MybatisFavoriteRepository(FavoriteMapper mapper) {
        super(mapper);
    }

    @Override
    public Optional<Favorite> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<Favorite> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(ids).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId) {
        FavoriteDO dataObject = lambdaQuery()
                .eq(FavoriteDO::getUserId, userId)
                .eq(FavoriteDO::getProductId, productId)
                .eq(FavoriteDO::getDelFlag, 0)
                .one();
        return Optional.ofNullable(toDomain(dataObject));
    }

    @Override
    public List<Favorite> findByUserId(Long userId, long offset, long limit) {
        long pageNum = offset / limit + 1;
        List<FavoriteDO> dataObjects = lambdaQuery()
                .eq(FavoriteDO::getUserId, userId)
                .eq(FavoriteDO::getDelFlag, 0)
                .orderByDesc(FavoriteDO::getCreateTime)
                .page(new Page<>(pageNum, limit))
                .getRecords();
        return dataObjects.stream().map(this::toDomain).toList();
    }

    @Override
    public long countByUserId(Long userId) {
        return lambdaQuery()
                .eq(FavoriteDO::getUserId, userId)
                .eq(FavoriteDO::getDelFlag, 0)
                .count();
    }

    @Override
    public Favorite save(Favorite favorite) {
        FavoriteDO softDeleted = mapper.selectSoftDeletedByUserIdAndProductId(
                favorite.getUserId(), favorite.getProductId());

        if (softDeleted != null) {
            mapper.reviveById(softDeleted.getId(), favorite.getUserId());
            FavoriteDO revived = mapper.selectById(softDeleted.getId());
            return Favorite.reconstitute(revived.getId(), revived.getUserId(), revived.getProductId(), revived.getCreateTime());
        }

        FavoriteDO dataObject = toDataObject(favorite);
        mapper.insert(dataObject);
        return Favorite.reconstitute(dataObject.getId(), dataObject.getUserId(), dataObject.getProductId(), dataObject.getCreateTime());
    }

    @Override
    public void removeById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public int removeByIds(List<Long> ids) {
        return mapper.deleteByIds(ids);
    }

    @Override
    public boolean existsByUserIdAndProductId(Long userId, Long productId) {
        return lambdaQuery()
                .eq(FavoriteDO::getUserId, userId)
                .eq(FavoriteDO::getProductId, productId)
                .eq(FavoriteDO::getDelFlag, 0)
                .count() > 0;
    }

    @Override
    public Set<Long> findFavoritedProductIds(Long userId, List<Long> productIds) {
        List<FavoriteDO> dataObjects = lambdaQuery()
                .eq(FavoriteDO::getUserId, userId)
                .in(FavoriteDO::getProductId, productIds)
                .eq(FavoriteDO::getDelFlag, 0)
                .list();
        return dataObjects.stream()
                .map(FavoriteDO::getProductId)
                .collect(Collectors.toSet());
    }

    private Favorite toDomain(FavoriteDO dataObject) {
        if (dataObject == null) return null;
        return Favorite.reconstitute(dataObject.getId(), dataObject.getUserId(), dataObject.getProductId(), dataObject.getCreateTime());
    }

    private FavoriteDO toDataObject(Favorite favorite) {
        if (favorite == null) return null;
        FavoriteDO dataObject = new FavoriteDO();
        dataObject.setId(favorite.getId());
        dataObject.setUserId(favorite.getUserId());
        dataObject.setProductId(favorite.getProductId());
        return dataObject;
    }
}