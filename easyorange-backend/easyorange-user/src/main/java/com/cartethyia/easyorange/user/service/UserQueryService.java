package com.cartethyia.easyorange.user.service;

import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.enums.LoginType;

public interface UserQueryService {

    User findUserByLoginType(String account, LoginType loginType);

    User findUserByAccount(String account);
}