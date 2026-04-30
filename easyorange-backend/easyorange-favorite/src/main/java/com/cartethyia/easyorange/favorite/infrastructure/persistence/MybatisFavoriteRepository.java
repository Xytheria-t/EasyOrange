package com.cartethyia.easyorange.favorite.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.favorite.domain.model.Favorite;
import com.cartethyia.easyorange.favorite.domain.repository.FavoriteRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MybatisFavoriteRepository extends ServiceImpl<FavoriteMapper, FavoriteDO> implements FavoriteRepository {

    @Override
    public Optional<Favorite> findById(Long id) {
        FavoriteDO dataObject = baseMapper.selectById(id);
        return Optional.ofNullable(toDomain(dataObject));
    }

    @Override
    public Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId) {
        FavoriteDO dataObject = baseMapper.selectOne(
                new LambdaQueryWrapper<FavoriteDO>()
                        .eq(FavoriteDO::getUserId, userId)
                        .eq(FavoriteDO::getProductId, productId)
                        .eq(FavoriteDO::getDelFlag, 0)
        );
        return Optional.ofNullable(toDomain(dataObject));
    }

    @Override
    public List<Favorite> findByUserId(Long userId, long offset, long limit) {
        long pageNum = offset / limit + 1;
        Page<FavoriteDO> page = new Page<>(pageNum, limit);
        List<FavoriteDO> dataObjects = baseMapper.selectPage(page,
                new LambdaQueryWrapper<FavoriteDO>()
                        .eq(FavoriteDO::getUserId, userId)
                        .eq(FavoriteDO::getDelFlag, 0)
                        .orderByDesc(FavoriteDO::getCreateTime))
                .getRecords();
        return dataObjects.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countByUserId(Long userId) {
        return baseMapper.selectCount(
                new LambdaQueryWrapper<FavoriteDO>()
                        .eq(FavoriteDO::getUserId, userId)
                        .eq(FavoriteDO::getDelFlag, 0)
        );
    }

    @Override
    public Favorite save(Favorite favorite) {
        FavoriteDO dataObject = toDataObject(favorite);
        baseMapper.insert(dataObject);
        return Favorite.reconstitute(dataObject.getId(), dataObject.getUserId(), dataObject.getProductId(), dataObject.getCreateTime());
    }

    @Override
    public void removeById(Long id) {
        baseMapper.deleteById(id);
    }

    @Override
    public int removeByIds(List<Long> ids) {
        return baseMapper.deleteByIds(ids);
    }

    @Override
    public boolean existsByUserIdAndProductId(Long userId, Long productId) {
        return baseMapper.selectCount(
                new LambdaQueryWrapper<FavoriteDO>()
                        .eq(FavoriteDO::getUserId, userId)
                        .eq(FavoriteDO::getProductId, productId)
                        .eq(FavoriteDO::getDelFlag, 0)
        ) > 0;
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
