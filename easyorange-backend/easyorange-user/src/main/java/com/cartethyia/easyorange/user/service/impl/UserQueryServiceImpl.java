package com.cartethyia.easyorange.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.enums.LoginType;
import com.cartethyia.easyorange.user.mapper.UserMapper;
import com.cartethyia.easyorange.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserMapper userMapper;

    @Override
    public User findUserByLoginType(String account, LoginType loginType) {
        LoginType type = loginType != null ? loginType : LoginType.USERNAME;

        if (LoginType.WECHAT.equals(type)) {
            throw new UnsupportedOperationException("微信登录暂不支持通过账号查找用户");
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        switch (type) {
            case EMAIL -> queryWrapper.eq(User::getEmail, account);
            case PHONE -> queryWrapper.eq(User::getPhone, account);
            case USERNAME -> queryWrapper.eq(User::getUsername, account);
            default -> throw new UnsupportedOperationException("不支持的登录类型: " + type);
        }

        return userMapper.selectOne(queryWrapper);
    }

    @Override
    public User findUserByAccount(String account) {
        if (account == null || account.isBlank()) {
            return null;
        }

        if (account.contains("@")) {
            return findUserByLoginType(account, LoginType.EMAIL);
        }
        if (account.matches("^1[3-9]\\d{9}$")) {
            return findUserByLoginType(account, LoginType.PHONE);
        }
        return findUserByLoginType(account, LoginType.USERNAME);
    }
}