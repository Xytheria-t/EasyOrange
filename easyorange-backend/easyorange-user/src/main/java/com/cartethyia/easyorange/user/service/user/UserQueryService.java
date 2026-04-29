package com.cartethyia.easyorange.user.service.user;

import com.cartethyia.easyorange.user.entity.User;

public interface UserQueryService {

    User findUserByAccount(String account);
}