package com.cartethyia.easyorange.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信小程序登录请求
 *
 * @author cartethyia
 * @date 2026/03/07
 */
@Data
public class MiniprogramLoginRequest {

    /**
     * 微信登录 code
     */
    @NotBlank(message = "微信登录 code 不能为空")
    private String code;
}
