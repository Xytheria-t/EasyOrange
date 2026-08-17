package com.cartethyia.easyorange.favorite.adapter.outbound.persistence;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import com.cartethyia.easyorange.favorite.domain.repository.FavoriteRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class FavoriteRepositoryImpl extends BaseRepository<FavoriteMapper, FavoriteDO> implements FavoriteRepository {

    private final IdGenerator idGenerator;

    public FavoriteRepositoryImpl(FavoriteMapper mapper, IdGenerator idGenerator) {
        super(mapper);
        this.idGenerator = idGenerator;
    }

    @Override
    public List<Favorite> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectByIds(ids).stream().map(this::toDomain).toList();
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
    public List<Favorite> findByProductId(String productId) {
        return lambdaQuery().eq(FavoriteDO::getProductId, productId).eq(FavoriteDO::getDelFlag, 0).list().stream()
                .map(this::toDomain)
                .toList();
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
        FavoriteDO softDeleted = mapper.selectSoftDeletedByUserIdAndProductId(favorite.userId(), favorite.productId());

        if (softDeleted != null) {
            mapper.reviveById(softDeleted.getId(), favorite.userId());
            FavoriteDO revived = mapper.selectById(softDeleted.getId());
            return Favorite.reconstitute(
                    revived.getId(),
                    revived.getUserId(),
                    revived.getProductId(),
                    revived.getPriceSnapshot(),
                    revived.getCreateTime());
        }

        FavoriteDO dataObject = toDataObject(favorite);
        dataObject.setId(idGenerator.generateId());
        mapper.insert(dataObject);
        return Favorite.reconstitute(
                dataObject.getId(),
                dataObject.getUserId(),
                dataObject.getProductId(),
                dataObject.getPriceSnapshot(),
                dataObject.getCreateTime());
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
                        .count()
                > 0;
    }

    @Override
    public Set<String> findFavoritedProductIds(String userId, List<String> productIds) {
        List<FavoriteDO> dataObjects = lambdaQuery()
                .eq(FavoriteDO::getUserId, userId)
                .in(FavoriteDO::getProductId, productIds)
                .eq(FavoriteDO::getDelFlag, 0)
                .list();
        return dataObjects.stream().map(FavoriteDO::getProductId).collect(Collectors.toSet());
    }

    @Override
    public boolean updatePriceSnapshot(String id, BigDecimal expectedSnapshot, BigDecimal newSnapshot) {
        return lambdaUpdate()
                .eq(FavoriteDO::getId, id)
                .eq(FavoriteDO::getDelFlag, 0)
                .nested(w -> {
                    if (expectedSnapshot == null) {
                        w.isNull(FavoriteDO::getPriceSnapshot);
                    } else {
                        w.eq(FavoriteDO::getPriceSnapshot, expectedSnapshot);
                    }
                })
                .set(FavoriteDO::getPriceSnapshot, newSnapshot)
                .update();
    }

    private Favorite toDomain(FavoriteDO dataObject) {
        return Favorite.reconstitute(
                dataObject.getId(),
                dataObject.getUserId(),
                dataObject.getProductId(),
                dataObject.getPriceSnapshot(),
                dataObject.getCreateTime());
    }

    private FavoriteDO toDataObject(Favorite favorite) {
        FavoriteDO dataObject = new FavoriteDO();
        dataObject.setId(favorite.id());
        dataObject.setUserId(favorite.userId());
        dataObject.setProductId(favorite.productId());
        dataObject.setPriceSnapshot(favorite.priceSnapshot());
        return dataObject;
    }
}
