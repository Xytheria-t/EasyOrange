package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
        @NotBlank(message = "分类名称不能为空") @Size(max = 20, message = "分类名称最长20个字符")
        String name,

        /**
         * 父分类 id；不传表示移到一级。
         * <p>
         * 与当前父分类不同即触发移动（连带平移整棵子树的层级），相同则只改属性 ——
         * 这个区分在服务端做，前端不必自己判断。
         */
        String parentId,

        String icon,

        @Min(value = 0, message = "排序值最小为0") @Max(value = 9999, message = "排序值最大为9999")
        Integer sortOrder,

        @Min(value = 0, message = "状态最小为0") @Max(value = 1, message = "状态最大为1")
        Integer status) {}
