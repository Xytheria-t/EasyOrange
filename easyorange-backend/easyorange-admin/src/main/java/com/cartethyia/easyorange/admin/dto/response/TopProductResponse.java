package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TopProductResponse {
    private Long productId;
    private String name;
    private Integer viewCount;
    private BigDecimal price;
    private String mainImage;
    private Integer status;
    private String statusDesc;
}