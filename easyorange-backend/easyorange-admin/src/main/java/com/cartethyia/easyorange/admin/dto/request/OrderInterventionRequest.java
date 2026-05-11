package com.cartethyia.easyorange.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderInterventionRequest {

    @NotBlank(message = "操作原因不能为空")
    private String reason;
}
