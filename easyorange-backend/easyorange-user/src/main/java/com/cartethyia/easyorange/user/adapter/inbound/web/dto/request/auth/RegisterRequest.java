package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth;

import com.cartethyia.easyorange.user.adapter.inbound.web.validation.Password;
import com.cartethyia.easyorange.user.adapter.inbound.web.validation.Phone;
import com.cartethyia.easyorange.user.adapter.inbound.web.validation.Username;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @Username
    String username,

    @NotBlank(message = "密码不能为空")
    @Password
    String password,

    @Phone
    String phone,

    @Email(message = "邮箱格式不正确")
    String email
) {}