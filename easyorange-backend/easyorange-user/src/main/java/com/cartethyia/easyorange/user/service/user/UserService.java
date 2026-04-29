package com.cartethyia.easyorange.user.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cartethyia.easyorange.user.dto.bo.*;
import com.cartethyia.easyorange.user.dto.vo.UserProfileVO;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.entity.User;

public interface UserService extends IService<User> {

    UserProfileVO getUserInfo();

    Long register(RegisterBo bo);

    UserVO updateUserInfo(UpdateUserBo bo);

    void changePassword(ChangePasswordBo bo);

    void forgotPassword(ForgotPasswordBo bo);

    UserVO uploadAvatar(UploadAvatarBo bo);
}