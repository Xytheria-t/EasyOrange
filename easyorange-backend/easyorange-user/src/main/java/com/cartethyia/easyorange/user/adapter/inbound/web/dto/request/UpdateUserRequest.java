package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Email(message = "邮箱格式不正确")
    String email,

    @Size(min = 11, max = 11, message = "手机号长度必须为 11 位")
    String phone,

    @Min(value = 0, message = "性别值无效")
    @Max(value = 2, message = "性别值无效")
    Integer gender
) {}
