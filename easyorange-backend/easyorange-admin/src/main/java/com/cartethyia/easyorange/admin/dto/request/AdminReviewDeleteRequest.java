package com.cartethyia.easyorange.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminReviewDeleteRequest {

    @NotBlank(message = "删除原因不能为空")
    private String reason;
}
