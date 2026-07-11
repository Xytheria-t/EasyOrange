package com.cartethyia.easyorange.favorite.adapter.outbound.persistence;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import com.cartethyia.easyorange.favorite.domain.repository.FavoriteRepository;
import com.cartethyia.easyorange.common.repository.BaseRepository;
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
    public Optional<Favorite> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<Favorite> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(ids).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public Optional<Favorite> findByUserIdAndProductId(String userId, String productId) {
        return Optional.ofNullable(lambdaQuery()
                        .eq(FavoriteDO::getUserId, userId)
                        .eq(FavoriteDO::getProductId, productId)
                        .eq(FavoriteDO::getDelFlag, 0)
                        .one())
                .map(this::toDomain);
    }

    @Override
    public List<Favorite> findByUserId(String userId, long offset, long limit) {
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
    public long countByUserId(String userId) {
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
    public void removeById(String id) {
        mapper.deleteById(id);
    }

    @Override
    public int removeByIds(List<String> ids) {
        return mapper.deleteByIds(ids);
    }

    @Override
    public boolean existsByUserIdAndProductId(String userId, String productId) {
        return lambdaQuery()
                .eq(FavoriteDO::getUserId, userId)
                .eq(FavoriteDO::getProductId, productId)
                .eq(FavoriteDO::getDelFlag, 0)
                .count() > 0;
    }

    @Override
    public Set<String> findFavoritedProductIds(String userId, List<String> productIds) {
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
        return Favorite.reconstitute(dataObject.getId(), dataObject.getUserId(), dataObject.getProductId(), dataObject.getCreateTime());
    }

    private FavoriteDO toDataObject(Favorite favorite) {
        FavoriteDO dataObject = new FavoriteDO();
        dataObject.setId(favorite.getId());
        dataObject.setUserId(favorite.getUserId());
        dataObject.setProductId(favorite.getProductId());
        return dataObject;
    }
}