package com.cartethyia.easyorange.favorite.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BatchRemoveRequest {
    @NotEmpty(message = "收藏ID列表不能为空")
    private List<Long> ids;
}
