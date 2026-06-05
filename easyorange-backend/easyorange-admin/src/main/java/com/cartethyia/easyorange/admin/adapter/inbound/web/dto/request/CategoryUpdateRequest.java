package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 20, message = "分类名称最长20个字符")
    String name,
    Long parentId,
    @Min(value = 0, message = "排序值最小为0")
    @Max(value = 9999, message = "排序值最大为9999")
    Integer sortOrder
) {}
