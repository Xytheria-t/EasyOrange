package com.cartethyia.easyorange.user.domain.repository;

import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    // ── 查询 ──

    Optional<User> findById(String id);

    List<User> findAllByIds(Collection<String> ids);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByUsername(String username);

    Optional<User> findByLoginIdentifier(String identifier);

    // ── 写入 ──

    User save(User user);

    void update(User user);

    // ── 聚合统计 ──

    long count();

    /** 统计指定类型的未删除用户数（管理端「最后一个管理员不可变更」保护）。 */
    long countByUserType(UserType userType);
}
