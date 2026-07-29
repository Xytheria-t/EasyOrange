package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.exception.ConcurrentUpdateException;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Primary
@Repository
public class UserRepositoryImpl extends BaseRepository<UserMapper, UserDO> implements UserRepository {

    private final UserEntityMapper entityMapper;

    public UserRepositoryImpl(UserMapper userMapper,
                              @Qualifier("userEntityMapperImpl") UserEntityMapper entityMapper) {
        super(userMapper);
        this.entityMapper = entityMapper;
    }

    // ========== Query methods ==========

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id))
            .map(entityMapper::toDomain);
    }

    @Override
    public List<User> findAllByIds(Collection<String> ids) {
        return findAllByIn(UserDO::getId, ids).stream()
            .map(entityMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return findBy(UserDO::getEmail, email).map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return findBy(UserDO::getPhone, phone).map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByStudentId(String studentId) {
        return findBy(UserDO::getStudentId, studentId).map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return findBy(UserDO::getUsername, username).map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByLoginIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        var trimmed = identifier.trim();
        return Optional.ofNullable(lambdaQuery()
                .eq(UserDO::getUsername, trimmed)
                .or()
                .eq(UserDO::getEmail, trimmed)
                .or()
                .eq(UserDO::getPhone, trimmed)
                .one())
                .map(entityMapper::toDomain);
    }

    // ========== Write methods ==========

    @Override
    public User save(User user) {
        UserDO entity = entityMapper.from(user);
        mapper.insert(entity);
        return user.assignId(entity.getId());
    }

    @Override
    public void update(User user) {
        UserDO entity = entityMapper.from(user);
        if (mapper.updateById(entity) == 0) {
            throw new ConcurrentUpdateException("用户更新冲突: id=" + user.getId());
        }
    }

    /**
     * 更新用户登录信息（IP和时间），使用独立事务确保登录记录不受主业务事务影响。
     * 即使用户注册/登录主流程失败，登录记录也应被保存用于安全审计。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updateLoginInfo(String userId, String loginIp) {
        boolean updated = lambdaUpdate()
            .eq(UserDO::getId, userId)
            .set(UserDO::getLoginDate, LocalDateTime.now())
            .set(UserDO::getLoginIp, loginIp)
            .update();
        if (!updated) {
            throw BusinessException.of(UserResultCode.USER_NOT_FOUND);
        }
    }

    @Override
    public void deleteById(String id) {
        mapper.deleteById(id);
    }

    // ========== Aggregate methods ==========

    @Override
    public long count() {
        return super.count();
    }
}
