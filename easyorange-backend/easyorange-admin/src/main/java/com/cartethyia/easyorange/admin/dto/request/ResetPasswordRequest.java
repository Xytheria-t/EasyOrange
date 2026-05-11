package com.cartethyia.easyorange.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "重置原因不能为空")
    private String reason;
}
