package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RecentProductVO {

    private Long productId;

    private String name;

    private BigDecimal price;

    private String mainImage;

    private Integer status;

    private String statusDesc;

    private Long sellerId;

    private String sellerName;

    private String categoryName;

    private Integer viewCount;

    private LocalDateTime createTime;
}
