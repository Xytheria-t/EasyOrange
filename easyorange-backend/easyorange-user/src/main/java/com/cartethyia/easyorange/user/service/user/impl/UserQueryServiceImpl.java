package com.cartethyia.easyorange.user.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.user.constant.UserConstant;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.mapper.UserMapper;
import com.cartethyia.easyorange.user.service.user.UserQueryService;
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

        boolean isEmail = UserConstant.EMAIL_PATTERN.matcher(account).matches();
        boolean isPhone = UserConstant.PHONE_PATTERN.matcher(account).matches();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (isEmail) {
            wrapper.eq(User::getEmail, account).or().eq(User::getPhone, account);
        } else if (isPhone) {
            wrapper.eq(User::getPhone, account);
        }

        wrapper.or().eq(User::getUsername, account);

        return userMapper.selectOne(wrapper);
    }
}