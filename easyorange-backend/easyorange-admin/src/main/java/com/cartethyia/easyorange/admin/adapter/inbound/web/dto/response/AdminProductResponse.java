package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record AdminProductResponse(
        String productId,
        String name,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer stock,
        String status,
        String statusDesc,
        String conditionLevel,
        String location,
        String contactMethod,
        List<String> images,
        String mainImage,
        String categoryId,
        String categoryName,
        String sellerId,
        String sellerName,
        String sellerAvatar,
        Integer viewCount,
        LocalDateTime createTime,
        LocalDateTime updateTime) {}
