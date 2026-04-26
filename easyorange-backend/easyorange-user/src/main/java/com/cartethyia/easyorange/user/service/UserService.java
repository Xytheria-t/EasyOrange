package com.cartethyia.easyorange.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cartethyia.easyorange.user.dto.request.*;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends IService<User> {

    UserVO getUserInfo();

    Long register(RegisterRequest request);

    UserVO updateUserInfo(UpdateUserRequest request);

    void changePassword(ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    UserVO uploadAvatar(MultipartFile avatar);
}
