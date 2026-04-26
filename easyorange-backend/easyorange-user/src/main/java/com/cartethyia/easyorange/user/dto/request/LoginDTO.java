package com.cartethyia.easyorange.user.dto.request;

import com.cartethyia.easyorange.user.constant.UserConstants;
import com.cartethyia.easyorange.user.enums.LoginMethod;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {

    private String clientType;

    private String loginMethod;

    @Size(min = UserConstants.USERNAME_MIN_LENGTH, max = UserConstants.USERNAME_MAX_LENGTH,
            message = "账号长度必须在" + UserConstants.USERNAME_MIN_LENGTH + "-" + UserConstants.USERNAME_MAX_LENGTH + "位之间")
    private String account;

    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    private String password;

    public LoginMethod getEffectiveLoginMethod() {
        return LoginMethod.fromValue(loginMethod);
    }
}
