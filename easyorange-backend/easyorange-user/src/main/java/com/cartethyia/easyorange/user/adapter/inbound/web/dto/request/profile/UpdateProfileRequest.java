package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.profile;

import com.cartethyia.easyorange.user.domain.constant.UserConstant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 30, message = "昵称长度不能超过 30 个字符") String nickname,

        @Email(message = "邮箱格式不正确") String email,

        @Pattern(regexp = UserConstant.PHONE_REGEX, message = "手机号格式不正确")
        String phone,

        @Pattern(regexp = "^[0-2]$", message = "性别值无效") String gender,

        @Size(max = 50, message = "真实姓名长度不能超过 50 个字符") String realName,

        @Size(max = 50, message = "学号长度不能超过 50 个字符") String studentId) {}
