package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AdminRatingResponse(
        String reviewId,
        String productId,
        String productName,
        String userId,
        String username,
        String userAvatar,
        Integer rating,
        String content,
        String replyContent,
        Integer likes,
        Integer status,
        LocalDateTime createTime,
        LocalDateTime updateTime) {}
