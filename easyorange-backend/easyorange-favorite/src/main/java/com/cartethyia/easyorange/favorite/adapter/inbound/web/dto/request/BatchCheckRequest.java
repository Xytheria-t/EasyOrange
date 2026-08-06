package com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchCheckRequest {
    @NotEmpty(message = "商品ID列表不能为空")
    private List<String> productIds;
}
