package com.cartethyia.easyorange.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProductAuditRequest {

    @NotNull(message = "审核动作不能为空")
    private Integer action;

    @Size(max = 500, message = "原因最长500字符")
    private String reason;

    private List<String> dimensions;

    @Size(max = 500, message = "备注最长500字符")
    private String remark;
}
