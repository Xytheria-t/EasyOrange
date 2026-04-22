package com.cartethyia.easyorange.user.domain.repository;

import com.cartethyia.easyorange.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserReadRepository {

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    List<User> findByIds(List<Long> ids);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}