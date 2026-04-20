package com.cartethyia.easyorange.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cartethyia.easyorange.user.dto.request.*;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.entity.User;

public interface UserService extends IService<User> {

    UserVO getUserInfo();

    void register(RegisterRequest request);

    UserVO updateUserInfo(UpdateUserRequest request);

    void changePassword(ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);
}
