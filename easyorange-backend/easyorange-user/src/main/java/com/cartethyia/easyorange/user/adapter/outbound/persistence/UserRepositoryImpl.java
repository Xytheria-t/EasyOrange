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
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Primary
@Repository
public class UserRepositoryImpl extends BaseRepository<UserMapper, UserDO> implements UserRepository {

    private final UserEntityMapper entityMapper;
    private final IdGenerator idGenerator;

    public UserRepositoryImpl(UserMapper userMapper, UserEntityMapper entityMapper, IdGenerator idGenerator) {
        super(userMapper);
        this.entityMapper = entityMapper;
        this.idGenerator = idGenerator;
    }

    // ── 查询 ──

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(entityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllByIds(Collection<String> ids) {
        return findAllByIn(UserDO::getId, ids).stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return findBy(UserDO::getEmail, email).map(entityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByPhone(String phone) {
        return findBy(UserDO::getPhone, phone).map(entityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return findBy(UserDO::getUsername, username).map(entityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByLoginIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        // 顺序探测 username/email/phone 三个唯一索引：OR 查询在 identifier 同时命中多行时
        // .one() 会抛异常，且结果依赖 index_merge 优化器决策；顺序探测结果确定、平均更快
        String trimmed = identifier.trim();
        return findByUsername(trimmed).or(() -> findByEmail(trimmed)).or(() -> findByPhone(trimmed));
    }

    // ── 写入 ──

    @Override
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
    public void update(User user) {
        UserDO entity = entityMapper.from(user);
        if (mapper.updateById(entity) == 0) {
            throw new ConcurrentUpdateException("用户更新冲突: id=" + user.getId());
        }
    }

    // ── 聚合统计 ──

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return super.count();
    }

    @Override
    @Transactional(readOnly = true)
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
