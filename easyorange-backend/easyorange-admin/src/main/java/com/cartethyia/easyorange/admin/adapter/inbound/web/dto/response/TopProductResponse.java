package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopProductResponse {
    private String productId;
    private String name;
    private Integer viewCount;
    private BigDecimal price;
    private String mainImage;
    private String status;
    private String statusDesc;
}
