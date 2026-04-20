package com.cartethyia.easyorange.user.dto.request;

import com.cartethyia.easyorange.user.constant.UserConstants;
import com.cartethyia.easyorange.user.enums.LoginType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import lombok.*;

/**
 * 登录请求参数
 *
 * @author cartethyia
 * @date 2026/03/07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * 用户账号（用户名/邮箱/手机号）
     * 当使用用户名登录时，账号需符合长度和字符限制
     */
    @NotBlank(message = "账号不能为空")
    @Size(min = UserConstants.USERNAME_MIN_LENGTH, max = UserConstants.USERNAME_MAX_LENGTH,
            message = "账号长度必须在" + UserConstants.USERNAME_MIN_LENGTH + "-" + UserConstants.USERNAME_MAX_LENGTH + "位之间")
    private String account;

    /**
     * 用户密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    @ToString.Exclude
    private String password;

    /**
     * 登录类型（USERNAME/EMAIL/PHONE），默认为 USERNAME
     */
    private LoginType loginType;
}
