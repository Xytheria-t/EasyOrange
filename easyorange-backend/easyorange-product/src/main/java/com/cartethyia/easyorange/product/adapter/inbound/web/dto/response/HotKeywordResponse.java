package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HotKeywordResponse(String id, String keyword, Integer searchCount, Integer hotLevel) {}
