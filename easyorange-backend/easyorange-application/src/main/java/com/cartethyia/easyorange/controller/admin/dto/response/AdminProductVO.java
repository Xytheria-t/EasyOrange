package com.cartethyia.easyorange.controller.admin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminProductVO {

    private Long productId;

    private String name;

    private String description;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer status;

    private String statusDesc;

    private Integer conditionLevel;

    private String location;

    private String contactMethod;

    private List<String> images;

    private String mainImage;

    private Long categoryId;

    private String categoryName;

    private Long sellerId;

    private String sellerName;

    private String sellerAvatar;

    private Integer viewCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
