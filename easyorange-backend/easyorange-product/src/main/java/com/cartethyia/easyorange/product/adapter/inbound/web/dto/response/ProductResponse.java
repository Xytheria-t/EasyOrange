package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductResponse(
        String id,
        String sellerId,
        String username,
        String userAvatar,
        String categoryId,
        String categoryName,
        String title,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer stock,
        String status,
        String statusDesc,
        Integer views,
        String condition,
        String conditionDesc,
        String location,
        String contactMethod,
        List<String> images,
        String mainImageUrl,
        LocalDateTime createTime,
        LocalDateTime updateTime) {}
