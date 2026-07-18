package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminRatingDeleteRequest {

    @NotBlank(message = "删除原因不能为空")
    private String reason;
}
