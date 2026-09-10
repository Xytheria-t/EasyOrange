package com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BatchRemoveRequest(
        @NotEmpty(message = "收藏ID列表不能为空") @Size(max = 100, message = "单次最多删除 100 条收藏")
        List<String> ids) {}
