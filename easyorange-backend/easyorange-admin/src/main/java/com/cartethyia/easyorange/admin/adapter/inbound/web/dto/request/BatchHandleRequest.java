package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class BatchHandleRequest {

    @NotEmpty(message = "举报ID列表不能为空")
    @Size(max = 50, message = "批量处理数量不能超过50条")
    private List<String> reportIds;

    @NotBlank(message = "处理动作不能为空")
    @Pattern(regexp = "^resolve|dismiss|IGNORE|PRODUCT_OFFLINE|WARN_SENDER|BAN_PRODUCT$", message = "无效的处理动作")
    private String action;

    private String remark;
}
