package com.cartethyia.easyorange.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.entity.User;
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
    public User findUserByAccount(String account) {
        if (account == null || account.isBlank()) {
            return null;
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        if (account.contains("@")) {
            BizRequire.validEmail(account, "邮箱格式不正确");
            queryWrapper.eq(User::getEmail, account);
        } else if (account.matches("^1[3-9]\\d{9}$")) {
            BizRequire.validPhone(account, "手机号格式不正确");
            queryWrapper.eq(User::getPhone, account);
        } else {
            BizRequire.between(account.length(), 1, 50, "用户名长度必须在 1-50 之间");
            queryWrapper.eq(User::getUsername, account);
        }

        return userMapper.selectOne(queryWrapper);
    }
}