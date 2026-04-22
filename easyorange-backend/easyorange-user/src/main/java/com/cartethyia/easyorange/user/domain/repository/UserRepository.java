package com.cartethyia.easyorange.user.domain.repository;

import com.cartethyia.easyorange.user.domain.aggregate.UserAggregate;
import com.cartethyia.easyorange.user.domain.valueobject.UserId;

import java.util.Optional;

public interface UserRepository {

    void save(UserAggregate user);

    void update(UserAggregate user);

    Optional<UserAggregate> findById(UserId id);

    Optional<UserAggregate> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}