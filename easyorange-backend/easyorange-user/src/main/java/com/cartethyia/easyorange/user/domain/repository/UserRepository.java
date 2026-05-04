package com.cartethyia.easyorange.user.domain.repository;

import com.cartethyia.easyorange.user.domain.aggregate.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long id);

    List<User> findAllById(Collection<Long> ids);

    Optional<User> findByUsername(String username);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    Optional<User> findByStudentId(String studentId);

    Optional<User> findByAccount(String account);

    User save(User user);

    boolean update(User user);

    boolean updateLoginInfo(Long userId, String loginIp);

    void deleteById(Long id);

    long count();
}
