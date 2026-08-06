package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth;

import com.cartethyia.easyorange.user.adapter.inbound.web.validation.Password;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "旧密码不能为空") String oldPassword,

        @NotBlank(message = "新密码不能为空") @Password String newPassword) {}
