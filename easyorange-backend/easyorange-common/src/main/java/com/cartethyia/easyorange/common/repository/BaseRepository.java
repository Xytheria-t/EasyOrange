package com.cartethyia.easyorange.common.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.common.exception.ConcurrentUpdateException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class BaseRepository<M extends BaseMapper<T>, T> {

    protected final M mapper;

    protected BaseRepository(M mapper) {
        this.mapper = mapper;
    }

    /**
     * 执行 updateById，更新 0 行时抛出 {@link ConcurrentUpdateException}。
     * <p>
     * 所有 Repository 实现类应使用此方法替代直接调用 {@code mapper.updateById(entity)}，
     * 避免在每个模块重复写 {@code if (rows == 0) throw ...} 检查。
     * </p>
     */
    protected int updateById(T entity) {
        int rows = mapper.updateById(entity);
        if (rows == 0) {
            throw new ConcurrentUpdateException("更新失败，数据已被修改或不存在");
        }
        return rows;
    }

    protected LambdaQueryChainWrapper<T> lambdaQuery() {
        return ChainWrappers.lambdaQueryChain(mapper);
    }

    protected LambdaUpdateChainWrapper<T> lambdaUpdate() {
        return ChainWrappers.lambdaUpdateChain(mapper);
    }

    protected <R> Optional<T> findBy(SFunction<T, R> column, R value) {
        if (value == null) return Optional.empty();
        return Optional.ofNullable(lambdaQuery().eq(column, value).one());
    }

    protected <R> List<T> findAllByIn(SFunction<T, R> column, Collection<R> values) {
        if (values == null || values.isEmpty()) return List.of();
        return lambdaQuery().in(column, values).list();
    }

    protected <R> boolean exists(SFunction<T, R> column, R value) {
        return value != null && lambdaQuery().eq(column, value).exists();
    }

    protected long count() {
        return lambdaQuery().count();
    }
}
