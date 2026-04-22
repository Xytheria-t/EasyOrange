package com.cartethyia.easyorange.user.adapter.out.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.user.domain.aggregate.UserAggregate;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.Email;
import com.cartethyia.easyorange.user.domain.valueobject.Nickname;
import com.cartethyia.easyorange.user.domain.valueobject.Phone;
import com.cartethyia.easyorange.user.domain.valueobject.UserId;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisUserRepository implements UserRepository {

    private final UserMapper userMapper;

    @Override
    public void save(UserAggregate user) {
        userMapper.insert(user.toEntity());
    }

    @Override
    public void update(UserAggregate user) {
        userMapper.updateById(user.toEntity());
    }

    @Override
    public Optional<UserAggregate> findById(UserId id) {
        User user = userMapper.selectById(id.value());
        return Optional.ofNullable(user).map(UserAggregate::fromEntity);
    }

    @Override
    public Optional<UserAggregate> findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        return Optional.ofNullable(user).map(UserAggregate::fromEntity);
    }

    @Override
    public boolean existsByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.exists(wrapper);
    }

    @Override
    public boolean existsByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return userMapper.exists(wrapper);
    }

    @Override
    public boolean existsByPhone(String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        return userMapper.exists(wrapper);
    }
}