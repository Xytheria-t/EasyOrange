package com.cartethyia.easyorange.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUnlockRequest {

    @NotBlank(message = "解锁原因不能为空")
    private String reason;
}
