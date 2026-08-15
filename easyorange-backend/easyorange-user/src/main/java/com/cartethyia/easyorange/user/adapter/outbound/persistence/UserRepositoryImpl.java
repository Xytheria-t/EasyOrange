package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.exception.ConcurrentUpdateException;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class UserRepositoryImpl extends BaseRepository<UserMapper, UserDO> implements UserRepository {

    private final UserEntityMapper entityMapper;
    private final IdGenerator idGenerator;

    public UserRepositoryImpl(
            UserMapper userMapper,
            @Qualifier("userEntityMapperImpl") UserEntityMapper entityMapper,
            IdGenerator idGenerator) {
        super(userMapper);
        this.entityMapper = entityMapper;
        this.idGenerator = idGenerator;
    }

    // ========== Query methods ==========

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(entityMapper::toDomain);
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

        // 顺序探测 username/email/phone 三个唯一索引：OR 查询在 identifier 同时命中多行时
        // .one() 会抛异常，且结果依赖 index_merge 优化器决策；顺序探测结果确定、平均更快
        String trimmed = identifier.trim();
        return findByUsername(trimmed).or(() -> findByEmail(trimmed)).or(() -> findByPhone(trimmed));
    }

    // ========== Write methods ==========

    @Override
    public User save(User user) {
        UserDO entity = entityMapper.from(user);
        // 主键为 IdType.INPUT，领域新建聚合无 ID → 由应用生成 UUID v7 后落库
        if (entity.getId() == null) {
            entity.setId(idGenerator.generateId());
        }
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

    // ========== Aggregate methods ==========

    @Override
    public long count() {
        return super.count();
    }

    @Override
    public long countByUserType(UserType userType) {
        if (userType == null) {
            return 0L;
        }
        return lambdaQuery()
                .eq(UserDO::getUserType, userType)
                .eq(UserDO::getDelFlag, 0)
                .count();
    }
}
