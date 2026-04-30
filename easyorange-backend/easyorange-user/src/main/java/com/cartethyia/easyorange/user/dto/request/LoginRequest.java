package com.cartethyia.easyorange.user.dto.request;

import com.cartethyia.easyorange.user.common.constant.UserConstant;
import com.cartethyia.easyorange.user.common.enums.ClientType;
import com.cartethyia.easyorange.user.common.enums.LoginMethod;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    private String clientType;

    private String loginMethod;

    @Size(min = UserConstant.USERNAME_MIN_LENGTH, max = UserConstant.USERNAME_MAX_LENGTH,
            message = "账号长度必须在" + UserConstant.USERNAME_MIN_LENGTH + "-" + UserConstant.USERNAME_MAX_LENGTH + "位之间")
    private String account;

    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 位之间")
    private String password;

    public LoginMethod getEffectiveLoginMethod() {
        return LoginMethod.fromValue(loginMethod);
    }

    public ClientType getEffectiveClientType() {
        return ClientType.fromCode(clientType);
    }
}
