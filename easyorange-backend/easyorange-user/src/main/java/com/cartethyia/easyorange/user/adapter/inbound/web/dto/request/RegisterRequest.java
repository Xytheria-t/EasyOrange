package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request;

import com.cartethyia.easyorange.user.domain.shared.constant.UserConstant;
import com.cartethyia.easyorange.user.adapter.inbound.web.validation.Password;
import com.cartethyia.easyorange.user.adapter.inbound.web.validation.Unique;
import jakarta.validation.constraints.*;

@Unique(field = "username", message = "用户名已存在")
public record RegisterRequest(
    @NotBlank(message = "用户名不能为空")
    @Size(
        min = UserConstant.USERNAME_MIN_LENGTH,
        max = UserConstant.USERNAME_MAX_LENGTH,
        message = "用户名长度必须在 " + UserConstant.USERNAME_MIN_LENGTH + "-" + UserConstant.USERNAME_MAX_LENGTH + " 位之间"
    )
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    String username,

    @NotBlank(message = "密码不能为空")
    @Size(
        min = UserConstant.PASSWORD_MIN_LENGTH,
        max = UserConstant.PASSWORD_MAX_LENGTH,
        message = "密码长度必须在 " + UserConstant.PASSWORD_MIN_LENGTH + "-" + UserConstant.PASSWORD_MAX_LENGTH + " 位之间"
    )
    @Password
    String password,

    @Pattern(regexp = UserConstant.PHONE_REGEX, message = "手机号格式不正确")
    String phone,

    @Email(message = "邮箱格式不正确")
    String email
) {}
