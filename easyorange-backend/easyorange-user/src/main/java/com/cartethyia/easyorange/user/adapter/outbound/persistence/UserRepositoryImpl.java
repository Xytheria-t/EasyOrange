package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserEntityMapper entityMapper;

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id))
            .map(entityMapper::toDomain);
    }

    @Override
    public List<User> findAllById(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.<UserEntity>lambdaQuery()
            .in(UserEntity::getId, ids);
        return userMapper.selectList(wrapper).stream()
            .map(entityMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return findOneBy(UserEntity::getUsername, username);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return findOneBy(UserEntity::getPhone, phone);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return findOneBy(UserEntity::getEmail, email);
    }

    @Override
    public Optional<User> findByStudentId(String studentId) {
        return findOneBy(UserEntity::getStudentId, studentId);
    }

    @Override
    public Optional<User> findByAccount(String account) {
        if (account == null || account.isBlank()) {
            return Optional.empty();
        }
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.<UserEntity>lambdaQuery()
            .eq(UserEntity::getUsername, account)
            .or().eq(UserEntity::getEmail, account)
            .or().eq(UserEntity::getPhone, account);
        return Optional.ofNullable(userMapper.selectOne(wrapper))
            .map(entityMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = entityMapper.from(user);
        int rows = userMapper.insert(entity);
        if (rows == 0) {
            throw new IllegalStateException("用户保存失败");
        }
        return user.assignId(entity.getId());
    }

    @Override
    public boolean update(User user) {
        UserEntity entity = entityMapper.from(user);
        return userMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean updateLoginInfo(Long userId, String loginIp) {
        LambdaUpdateWrapper<UserEntity> wrapper = Wrappers.<UserEntity>lambdaUpdate()
            .eq(UserEntity::getId, userId)
            .set(UserEntity::getLoginDate, LocalDateTime.now())
            .set(UserEntity::getLoginIp, loginIp);
        return userMapper.update(null, wrapper) > 0;
    }

    @Override
    public void deleteById(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    public long count() {
        return userMapper.selectCount(null);
    }

    private <X> Optional<User> findOneBy(SFunction<UserEntity, X> column, X value) {
        if (value == null) {
            return Optional.empty();
        }
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.<UserEntity>lambdaQuery()
            .eq(column, value);
        return Optional.ofNullable(userMapper.selectOne(wrapper))
            .map(entityMapper::toDomain);
    }
}
