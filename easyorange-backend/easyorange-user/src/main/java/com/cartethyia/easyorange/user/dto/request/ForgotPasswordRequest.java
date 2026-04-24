package com.cartethyia.easyorange.user.dto.request;

import com.cartethyia.easyorange.user.constant.UserConstants;
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
    @Pattern(regexp = UserConstants.PHONE_REGEX, message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "新密码不能为空")
    @Size(
        min = UserConstants.PASSWORD_MIN_LENGTH,
        max = UserConstants.PASSWORD_MAX_LENGTH,
        message = "密码长度必须在 " + UserConstants.PASSWORD_MIN_LENGTH + "-" + UserConstants.PASSWORD_MAX_LENGTH + " 位之间"
    )
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,20}$",
        message = "密码必须包含大小写字母和数字"
    )
    private String newPassword;
}
