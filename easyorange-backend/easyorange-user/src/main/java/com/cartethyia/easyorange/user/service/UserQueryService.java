package com.cartethyia.easyorange.user.service;

import com.cartethyia.easyorange.user.entity.User;

public interface UserQueryService {

    User findUserByAccount(String account);
}