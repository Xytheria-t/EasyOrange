package com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchRemoveRequest {
    @NotEmpty(message = "收藏ID列表不能为空")
    private List<String> ids;
}
