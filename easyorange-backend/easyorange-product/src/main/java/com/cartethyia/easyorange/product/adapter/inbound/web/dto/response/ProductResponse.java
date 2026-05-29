package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {
    private Long id;
    private Long sellerId;
    private String username;
    private String userAvatar;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer status;
    private String statusDesc;
    private Integer views;
    private Integer condition;
    private String conditionDesc;
    private String location;
    private String contactMethod;
    private List<String> images;
    private String mainImageUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
