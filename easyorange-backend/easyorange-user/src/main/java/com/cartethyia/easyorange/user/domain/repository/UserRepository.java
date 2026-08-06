package com.cartethyia.easyorange.user.domain.repository;

import com.cartethyia.easyorange.user.domain.aggregate.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    // ========== Query methods ==========

    Optional<User> findById(String id);

    List<User> findAllByIds(Collection<String> ids);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByStudentId(String studentId);

    Optional<User> findByUsername(String username);

    Optional<User> findByLoginIdentifier(String identifier);

    // ========== Write methods ==========

    User save(User user);

    void update(User user);

    void updateLoginInfo(String userId, String loginIp);

    void deleteById(String id);

    // ========== Aggregate methods ==========

    long count();
}
