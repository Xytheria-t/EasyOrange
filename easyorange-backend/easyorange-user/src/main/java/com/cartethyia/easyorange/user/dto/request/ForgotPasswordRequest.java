package com.cartethyia.easyorange.user.dto.request;

import com.cartethyia.easyorange.user.constant.UserConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 忘记密码请求参数
 *
 * @author cartethyia
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = UserConstant.PHONE_REGEX, message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "新密码不能为空")
    @Size(
        min = UserConstant.PASSWORD_MIN_LENGTH,
        max = UserConstant.PASSWORD_MAX_LENGTH,
        message = "密码长度必须在 " + UserConstant.PASSWORD_MIN_LENGTH + "-" + UserConstant.PASSWORD_MAX_LENGTH + " 位之间"
    )
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,20}$",
        message = "密码必须包含大小写字母和数字"
    )
    private String newPassword;
}
