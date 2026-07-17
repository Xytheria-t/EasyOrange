package com.cartethyia.easyorange.common.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class BaseRepository<M extends BaseMapper<T>, T> {

    protected final M mapper;

    protected BaseRepository(M mapper) {
        this.mapper = mapper;
    }

    protected LambdaQueryChainWrapper<T> lambdaQuery() {
        return ChainWrappers.lambdaQueryChain(mapper);
    }

    protected LambdaUpdateChainWrapper<T> lambdaUpdate() {
        return ChainWrappers.lambdaUpdateChain(mapper);
    }

    protected <R> Optional<T> findBy(SFunction<T, R> column, R value) {
        return value == null ? Optional.empty() : Optional.ofNullable(lambdaQuery().eq(column, value).one());
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
