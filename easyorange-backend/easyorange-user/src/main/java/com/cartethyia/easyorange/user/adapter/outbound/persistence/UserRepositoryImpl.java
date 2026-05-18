package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import com.cartethyia.easyorange.framework.repository.BaseRepository;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl extends BaseRepository<UserMapper, UserEntity> implements UserRepository {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final UserEntityMapper entityMapper;

    public UserRepositoryImpl(UserMapper userMapper, UserEntityMapper entityMapper) {
        super(userMapper);
        this.entityMapper = entityMapper;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id))
            .map(entityMapper::toDomain);
    }

    @Override
    public List<User> findAllById(Collection<Long> ids) {
        return findIn(UserEntity::getId, ids).stream()
            .map(entityMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return findOne(UserEntity::getUsername, username).map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return findOne(UserEntity::getPhone, phone).map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return findOne(UserEntity::getEmail, email).map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByStudentId(String studentId) {
        return findOne(UserEntity::getStudentId, studentId).map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByAccount(String account) {
        if (account == null || account.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(lambdaQuery()
            .eq(UserEntity::getUsername, account)
            .or().eq(UserEntity::getEmail, account)
            .or().eq(UserEntity::getPhone, account)
            .one())
            .map(entityMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = entityMapper.from(user);
        int rows = mapper.insert(entity);
        if (rows == 0) {
            throw new IllegalStateException("用户保存失败");
        }
        return user.assignId(entity.getId());
    }

    @Override
    public boolean update(User user) {
        UserEntity entity = entityMapper.from(user);
        return mapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean updateLoginInfo(Long userId, String loginIp) {
        return lambdaUpdate()
            .eq(UserEntity::getId, userId)
            .set(UserEntity::getLoginDate, LocalDateTime.now())
            .set(UserEntity::getLoginIp, loginIp)
            .update();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public long count() {
        return mapper.selectCount(null);
    }
}