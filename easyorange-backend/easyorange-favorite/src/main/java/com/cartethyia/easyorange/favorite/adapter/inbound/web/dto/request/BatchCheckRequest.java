package com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchCheckRequest {
    @NotEmpty(message = "商品ID列表不能为空")
    @Size(max = 100, message = "单次最多检查 100 个商品")
    private List<String> productIds;
}
