package com.cartethyia.easyorange.product.application.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewVO {

    private Long id;

    private Long productId;

    private Long userId;

    private String username;

    private String userAvatar;

    private Integer rating;

    private String content;

    private Integer likes;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
