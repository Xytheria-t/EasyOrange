package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import com.cartethyia.easyorange.framework.exception.ConcurrentUpdateException;
import com.cartethyia.easyorange.framework.repository.BaseRepository;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl extends BaseRepository<UserMapper, UserEntity> implements UserRepository {

    private final UserEntityMapper entityMapper;

    public UserRepositoryImpl(UserMapper userMapper,
                              @Qualifier("userEntityMapperImpl") UserEntityMapper entityMapper) {
        super(userMapper);
        this.entityMapper = entityMapper;
    }

    // ========== Query methods ==========

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id))
            .map(entityMapper::toDomain);
    }

    @Override
    public List<User> findAllByIds(Collection<Long> ids) {
        return findAllByIn(UserEntity::getId, ids).stream()
            .map(entityMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return findBy(UserEntity::getEmail, email).map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return findBy(UserEntity::getPhone, phone).map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByStudentId(String studentId) {
        return findBy(UserEntity::getStudentId, studentId).map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return findBy(UserEntity::getUsername, username).map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByLoginIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String trimmedIdentifier = identifier.trim();
        return Optional.ofNullable(lambdaQuery()
                .and(wrapper -> wrapper
                        .eq(UserEntity::getUsername, trimmedIdentifier)
                        .or()
                        .eq(UserEntity::getEmail, trimmedIdentifier)
                        .or()
                        .eq(UserEntity::getPhone, trimmedIdentifier))
                .one())
                .map(entityMapper::toDomain);
    }

    // ========== Write methods ==========

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
    public void update(User user) {
        UserEntity entity = entityMapper.from(user);
        updateById(entity);
    }

    /**
     * 更新用户登录信息（IP和时间），使用独立事务确保登录记录不受主业务事务影响。
     * 即使用户注册/登录主流程失败，登录记录也应被保存用于安全审计。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updateLoginInfo(Long userId, String loginIp) {
        boolean updated = lambdaUpdate()
            .eq(UserEntity::getId, userId)
            .set(UserEntity::getLoginDate, LocalDateTime.now())
            .set(UserEntity::getLoginIp, loginIp)
            .update();
        if (!updated) {
            throw new ConcurrentUpdateException("更新登录信息失败，用户可能已被删除");
        }
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    // ========== Aggregate methods ==========

    @Override
    public long count() {
        return super.count();
    }
}
