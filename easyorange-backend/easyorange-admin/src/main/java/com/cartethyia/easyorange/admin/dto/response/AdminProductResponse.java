package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AdminProductResponse(
    Long productId,
    String name,
    String description,
    BigDecimal price,
    BigDecimal originalPrice,
    Integer stock,
    Integer status,
    String statusDesc,
    Integer conditionLevel,
    String location,
    String contactMethod,
    List<String> images,
    String mainImage,
    Long categoryId,
    String categoryName,
    Long sellerId,
    String sellerName,
    String sellerAvatar,
    Integer viewCount,
    LocalDateTime createTime,
    LocalDateTime updateTime
) {}