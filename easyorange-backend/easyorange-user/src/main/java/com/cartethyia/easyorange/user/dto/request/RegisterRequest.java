package com.cartethyia.easyorange.user.dto.request;

import com.cartethyia.easyorange.user.constant.UserConstants;
import com.cartethyia.easyorange.user.validation.Unique;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * 用户注册请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(
        min = UserConstants.USERNAME_MIN_LENGTH,
        max = UserConstants.USERNAME_MAX_LENGTH,
        message = "用户名长度必须在 " + UserConstants.USERNAME_MIN_LENGTH + "-" + UserConstants.USERNAME_MAX_LENGTH + " 位之间"
    )
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    @Unique(field = "username", message = "用户名已存在")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(
        min = UserConstants.PASSWORD_MIN_LENGTH,
        max = UserConstants.PASSWORD_MAX_LENGTH,
        message = "密码长度必须在 " + UserConstants.PASSWORD_MIN_LENGTH + "-" + UserConstants.PASSWORD_MAX_LENGTH + " 位之间"
    )
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,20}$",
        message = "密码必须包含大小写字母和数字"
    )
    private String password;
}
