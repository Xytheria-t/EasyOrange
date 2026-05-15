package com.cartethyia.easyorange.product.adapter.inbound.web.dto.request;

import com.cartethyia.easyorange.product.domain.enums.ReportReasonType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportRequest {

    @NotNull(message = "举报类型不能为空")
    private Integer reasonType;

    @NotBlank(message = "举报描述不能为空")
    @Size(max = 500, message = "举报描述不能超过500字")
    private String reason;

    public boolean isValidReasonType() {
        return ReportReasonType.isValidCode(reasonType);
    }
}
