package com.cartethyia.easyorange.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserRoleRequest {

    @NotBlank(message = "目标角色不能为空")
    @Pattern(regexp = "^01|02$", message = "角色值无效（01-普通用户，02-管理员）")
    private String role;

    @NotBlank(message = "变更原因不能为空")
    private String reason;
}
